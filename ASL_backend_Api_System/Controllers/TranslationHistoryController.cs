using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SignLanguageAPI.Data;
using SignLanguageAPI.Models;

namespace SignLanguageAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class TranslationHistoryController : ControllerBase
    {
        private readonly AppDbContext _context;

        public TranslationHistoryController(AppDbContext context)
        {
            _context = context;
        }

        [HttpPost]
        public async Task<IActionResult> Create([FromBody] CreateTranslationHistoryDto request)
        {
            await DeleteExpiredHistoryAsync();

            if (request == null)
                return BadRequest(new { message = "Invalid request" });

            if (!Guid.TryParse(request.UserId, out var userId))
                return BadRequest(new { message = "Valid UserId is required" });

            var sentence = (request.Sentence ?? "").Trim();
            if (string.IsNullOrWhiteSpace(sentence))
                return BadRequest(new { message = "Sentence is required" });

            var userExists = await _context.users.AnyAsync(u => u.user_id == userId);
            if (!userExists)
                return NotFound(new { message = "User not found" });

            var words = request.Words?
                .Select(w => (w ?? "").Trim())
                .Where(w => !string.IsNullOrWhiteSpace(w))
                .ToList() ?? new List<string>();

            if (words.Count == 0)
            {
                words = sentence
                    .Split(' ', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
                    .ToList();
            }

            var history = new TranslationHistory
            {
                HistoryId = Guid.NewGuid(),
                UserId = userId,
                Sentence = sentence,
                Words = words,
                TranslationType = string.IsNullOrWhiteSpace(request.TranslationType)
                    ? "Sign-to-Text"
                    : request.TranslationType.Trim(),
                CreatedAt = DateTime.UtcNow
            };

            _context.TranslationHistory.Add(history);
            await _context.SaveChangesAsync();

            return Ok(history);
        }

        [HttpGet("user/{userId:guid}")]
        public async Task<IActionResult> GetByUser(Guid userId)
        {
            await DeleteExpiredHistoryAsync();
            var cutoff = DateTime.UtcNow.AddHours(-24);
            var items = await _context.TranslationHistory
                .Where(h => h.UserId == userId && h.CreatedAt >= cutoff)
                .OrderByDescending(h => h.CreatedAt)
                .ToListAsync();

            return Ok(items);
        }

        [HttpGet("admin")]
        public async Task<IActionResult> GetAllForAdmin([FromQuery] string? type = null)
        {
            await DeleteExpiredHistoryAsync();
            var cutoff = DateTime.UtcNow.AddHours(-24);
            var query =
                from history in _context.TranslationHistory
                join user in _context.users on history.UserId equals user.user_id
                where history.CreatedAt >= cutoff
                select new
                {
                    history_id = history.HistoryId,
                    user_id = history.UserId,
                    full_name = user.full_name,
                    email = user.email,
                    sentence = history.Sentence,
                    words = history.Words,
                    translation_type = history.TranslationType,
                    created_at = history.CreatedAt
                };

            if (!string.IsNullOrWhiteSpace(type))
            {
                var normalizedType = type.Trim();
                query = query.Where(h => h.translation_type == normalizedType);
            }

            var items = await query
                .OrderByDescending(h => h.created_at)
                .ToListAsync();

            return Ok(items);
        }

        private async Task DeleteExpiredHistoryAsync()
        {
            var cutoff = DateTime.UtcNow.AddHours(-24);
            var expired = await _context.TranslationHistory
                .Where(h => h.CreatedAt < cutoff)
                .ToListAsync();

            if (expired.Count == 0)
                return;

            _context.TranslationHistory.RemoveRange(expired);
            await _context.SaveChangesAsync();
        }
    }
}
