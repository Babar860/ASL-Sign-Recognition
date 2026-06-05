using System;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json.Serialization;

namespace SignLanguageAPI.Models
{
    [Table("users")]
    public class users
    {
        [Key]
        [Column("user_id")]
        [DatabaseGenerated(DatabaseGeneratedOption.Identity)] // ✅ DB will generate UUID
        [JsonIgnore]
        public Guid user_id { get; set; }

        [Required]
        [Column("full_name")]
        public string full_name { get; set; } = string.Empty;

        [Required]
        [EmailAddress]
        [Column("email")]
        public string email { get; set; } = string.Empty;

        [Required]
        [Column("password_hash")]
        public string password_hash { get; set; } = string.Empty;

        // ✅ NEW: store salt so login can verify password correctly
        // Base64 string of the salt bytes used in PBKDF2
        [Column("password_salt")]
        public string? password_salt { get; set; }


        // ✅ Optional columns (can be NULL in DB)
        [Column("security_question")]
        public string? security_question { get; set; }

        [Column("security_answer_hash")]
        public string? security_answer_hash { get; set; }
    }

    // ✅ Verify security answer request
    public class VerifyAnswerDto
    {
        [Required]
        public string Email { get; set; } = string.Empty;

        [Required]
        public string Answer { get; set; } = string.Empty;
    }

    // ✅ Reset password request
    public class ResetPasswordDto
    {
        [Required]
        public string Email { get; set; } = string.Empty;

        [Required]
        public string NewPassword { get; set; } = string.Empty;
    }
}
