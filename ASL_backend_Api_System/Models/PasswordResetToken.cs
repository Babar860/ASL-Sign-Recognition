using System;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SignLanguageAPI.Models
{
    [Table("password_reset_tokens")]
    public class PasswordResetToken
    {
        [Key]
        public Guid id { get; set; }

        public Guid user_id { get; set; }

        public string token { get; set; }

        public DateTime expires_at { get; set; }

        public bool is_used { get; set; } = false;
    }
}