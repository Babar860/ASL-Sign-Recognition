using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Http;
using SignLanguageAPI.Data;
using SignLanguage.Models;

using System.Net.Http;
using System.Net.Http.Json;

namespace SignLanguage.Controllers
{
    public class VideoUploadRequest
    {
        public required IFormFile File { get; set; }

        // ✅ string for stable multipart binding
        public string? UserId { get; set; }

        public int? DurationSeconds { get; set; }
    }

    [ApiController]
    [Route("api/[controller]")]
    public class VideosController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly IWebHostEnvironment _env;

        public VideosController(AppDbContext context, IWebHostEnvironment env)
        {
            _context = context;
            _env = env;
        }

        public class UploadResponse
        {
            public bool Success { get; set; }
            public string Message { get; set; }
            public Guid? VideoId { get; set; }
            public string? FileUrl { get; set; }

            public UploadResponse(bool success, string message, Guid? videoId, string? fileUrl)
            {
                Success = success;
                Message = message;
                VideoId = videoId;
                FileUrl = fileUrl;
            }
        }

        // ✅ NEW: Python predict response model
        public class PythonPredictResponse
        {
            public string? predicted_text { get; set; }
        }

        [HttpPost("upload")]
        [RequestSizeLimit(200L * 1024 * 1024)] // 200 MB
        [RequestFormLimits(MultipartBodyLengthLimit = 200L * 1024 * 1024)]
        public async Task<IActionResult> Upload([FromForm] VideoUploadRequest request)
        {
            // ✅ DEBUG LOGS (Very important)
            Console.WriteLine("===== VIDEO UPLOAD DEBUG =====");
            Console.WriteLine($"UserId received from client: {request.UserId}");
            Console.WriteLine($"UserId (raw): '{request.UserId}'");
            Console.WriteLine($"DurationSeconds: {request.DurationSeconds}");
            Console.WriteLine($"File name: {request.File?.FileName}");
            Console.WriteLine($"File length: {request.File?.Length}");
            Console.WriteLine("================================");

            if (request.File == null || request.File.Length == 0)
                return BadRequest(new UploadResponse(false, "No file uploaded", null, null));

            try
            {
                // ✅ 1) uploads folder create
                var uploadsRoot = Path.Combine(_env.ContentRootPath, "Uploads");
                Directory.CreateDirectory(uploadsRoot);

                // ✅ 2) unique file name
                var ext = Path.GetExtension(request.File.FileName);
                var safeExt = string.IsNullOrWhiteSpace(ext) ? ".mp4" : ext;
                var uniqueFileName = $"{Guid.NewGuid()}{safeExt}";
                var filePath = Path.Combine(uploadsRoot, uniqueFileName);

                // ✅ 3) save file to disk
                using (var fs = new FileStream(filePath, FileMode.Create))
                {
                    await request.File.CopyToAsync(fs);
                }

                // ✅ 4) file_url for DB (relative URL)
                // NOTE: yahan "/uploads" use ho raha hai (lowercase)
                // Disk folder tumhara "Uploads" (capital U) hai
                var fileUrl = $"/uploads/{uniqueFileName}";

                // ✅ 5) duration (client de to use, warna 0)
                var duration = request.DurationSeconds ?? 0;

                // ✅ 6) SAFE parse UserId string -> Guid?
                Guid? parsedUserId = null;
                if (!string.IsNullOrWhiteSpace(request.UserId) &&
                    Guid.TryParse(request.UserId.Trim(), out var g))
                {
                    parsedUserId = g;
                }

                Console.WriteLine($"ParsedUserId: {(parsedUserId.HasValue ? parsedUserId.ToString() : "NULL")}");

                // ✅ 7) save DB record
                var video = new Video
                {
                    VideoId = Guid.NewGuid(),
                    UserId = parsedUserId,     // ✅ Guid? value
                    FileUrl = fileUrl,
                    DurationSeconds = duration
                };

                _context.Videos.Add(video);
                await _context.SaveChangesAsync();

                return Ok(new UploadResponse(true, "Video uploaded successfully", video.VideoId, fileUrl));
            }
            catch (Exception ex)
            {
                return StatusCode(500, new UploadResponse(false, $"Error: {ex.Message}", null, null));
            }
        }

        // ✅ NEW: Predict endpoint (DB -> disk path -> Python API -> text)
        // Call example:
        // POST /api/videos/{videoId}/predict
        [HttpPost("{videoId:guid}/predict")]
        public async Task<IActionResult> Predict(Guid videoId)
        {
            // 1) DB se video record
            var video = await _context.Videos.FindAsync(videoId);
            if (video == null)
                return NotFound(new { message = "Video not found" });

            if (string.IsNullOrWhiteSpace(video.FileUrl))
                return BadRequest(new { message = "FileUrl missing in DB" });

            // 2) FileUrl -> filename -> disk full path
            // DB: "/uploads/abc.mp4"  => filename "abc.mp4"
            var fileName = Path.GetFileName(video.FileUrl.Replace("\\", "/"));
            var fullPath = Path.Combine(_env.ContentRootPath, "Uploads", fileName);

            if (!System.IO.File.Exists(fullPath))
                return NotFound(new { message = "Video file not found on disk", fullPath });

            // 3) Python inference API call
            // IMPORTANT:
            // - jab Python service same PC pe run ho: 127.0.0.1 ok
            // - agar Python service dusre machine/server pe ho, yahan uska IP/URL use karo
            var pythonUrl = "http://127.0.0.1:8000/predict";

            try
            {
                using var http = new HttpClient();
                var payload = new { video_path = fullPath };

                var resp = await http.PostAsJsonAsync(pythonUrl, payload);

                if (!resp.IsSuccessStatusCode)
                {
                    var err = await resp.Content.ReadAsStringAsync();
                    return StatusCode((int)resp.StatusCode, new { message = "Python inference failed", error = err });
                }

                var py = await resp.Content.ReadFromJsonAsync<PythonPredictResponse>();
                var predicted = py?.predicted_text ?? "";

                return Ok(new
                {
                    videoId = video.VideoId,
                    predicted_text = predicted
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Error calling Python inference", error = ex.Message });
            }
        }

        [HttpGet]
        public IActionResult GetAll()
        {
            var videos = _context.Videos
                .OrderByDescending(v => v.VideoId)
                .ToList();

            return Ok(videos);
        }
    }
}