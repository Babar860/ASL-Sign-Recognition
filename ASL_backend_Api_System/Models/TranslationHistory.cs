using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SignLanguageAPI.Models
{
    [Table("translation_history")]
    public class TranslationHistory
    {
        [Key]
        [Column("history_id")]
        public Guid HistoryId { get; set; }

        [Required]
        [Column("user_id")]
        public Guid UserId { get; set; }

        [Required]
        [Column("sentence")]
        public string Sentence { get; set; } = string.Empty;

        [Column("words", TypeName = "text[]")]
        public List<string> Words { get; set; } = new();

        [Required]
        [Column("translation_type")]
        public string TranslationType { get; set; } = "Sign-to-Text";

        [Column("created_at")]
        public DateTime CreatedAt { get; set; }
    }

    public class CreateTranslationHistoryDto
    {
        public string? UserId { get; set; }
        public string? Sentence { get; set; }
        public List<string>? Words { get; set; }
        public string? TranslationType { get; set; }
    }
}
