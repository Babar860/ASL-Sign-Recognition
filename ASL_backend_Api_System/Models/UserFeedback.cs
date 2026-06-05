using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SignLanguageAPI.Models
{
    [Table("user_feedback")]
    public class UserFeedback
    {
        [Key]
        [Column("feedback_id")]
        public Guid FeedbackId { get; set; }

        [Required]
        [Column("user_id")]
        public Guid UserId { get; set; }

        [Column("feedbacks", TypeName = "text[]")]
        public List<string> Feedbacks { get; set; } = new();

        [Column("created_at")]
        public DateTime CreatedAt { get; set; }

        [Column("updated_at")]
        public DateTime UpdatedAt { get; set; }
    }

    public class AddFeedbackDto
    {
        public string? UserId { get; set; }
        public string? Feedback { get; set; }
    }
}
