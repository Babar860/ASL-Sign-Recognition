using System.ComponentModel.DataAnnotations;

namespace SignLanguageAPI.Models
{
    public class EditProfileDto
    {
        [Required]
        public string full_name { get; set; } = string.Empty;

        [Required]
        [EmailAddress]
        public string email { get; set; } = string.Empty;
    }
}