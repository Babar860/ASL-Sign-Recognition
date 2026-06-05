using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SignLanguageAPI.Data;
using SignLanguageAPI.Models;

namespace SignLanguageAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class FeedbackController : ControllerBase
    {
        private readonly AppDbContext _context;

        public FeedbackController(AppDbContext context)
        {
            _context = context;
        }

        [HttpPost]
        public async Task<IActionResult> Add([FromBody] AddFeedbackDto request)
        {
            if (request == null)
                return BadRequest(new { message = "Invalid request" });

            if (!Guid.TryParse(request.UserId, out var userId))
                return BadRequest(new { message = "Valid UserId is required" });

            var text = (request.Feedback ?? "").Trim();
            if (string.IsNullOrWhiteSpace(text))
                return BadRequest(new { message = "Feedback is required" });

            var userExists = await _context.users.AnyAsync(u => u.user_id == userId);
            if (!userExists)
                return NotFound(new { message = "User not found" });

            var row = await _context.UserFeedback.FirstOrDefaultAsync(f => f.UserId == userId);
            if (row == null)
            {
                row = new UserFeedback
                {
                    FeedbackId = Guid.NewGuid(),
                    UserId = userId,
                    Feedbacks = new List<string>(),
                    CreatedAt = DateTime.UtcNow,
                    UpdatedAt = DateTime.UtcNow
                };
                _context.UserFeedback.Add(row);
            }

            row.Feedbacks = row.Feedbacks.Append(text).ToList();
            row.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();

            return Ok(row);
        }

        [HttpGet("user/{userId:guid}")]
        public async Task<IActionResult> GetByUser(Guid userId)
        {
            var row = await _context.UserFeedback.FirstOrDefaultAsync(f => f.UserId == userId);
            return Ok(row ?? new UserFeedback
            {
                FeedbackId = Guid.Empty,
                UserId = userId,
                Feedbacks = new List<string>(),
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            });
        }

        [HttpGet]
        public async Task<IActionResult> GetAll()
        {
            var rows = await _context.UserFeedback
                .OrderByDescending(f => f.UpdatedAt)
                .ToListAsync();

            return Ok(rows);
        }

        [HttpGet("admin")]
        public async Task<IActionResult> GetAllForAdmin()
        {
            var rows = await (
                from feedback in _context.UserFeedback
                join user in _context.users on feedback.UserId equals user.user_id
                orderby feedback.UpdatedAt descending
                select new
                {
                    feedback_id = feedback.FeedbackId,
                    user_id = feedback.UserId,
                    full_name = user.full_name,
                    email = user.email,
                    feedbacks = feedback.Feedbacks,
                    created_at = feedback.CreatedAt,
                    updated_at = feedback.UpdatedAt
                }
            ).ToListAsync();

            return Ok(rows);
        }
    }
}
