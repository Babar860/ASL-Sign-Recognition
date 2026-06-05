namespace SignLanguageAPI.Models
{
    public class ConfirmPasswordResetDto
    {
        public string Token { get; set; }
        public string NewPassword { get; set; }
        public string ConfirmPassword { get; set; }
    }
}