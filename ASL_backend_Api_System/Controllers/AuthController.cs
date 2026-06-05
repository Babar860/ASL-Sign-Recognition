using Microsoft.AspNetCore.Mvc;
using SignLanguageAPI.Data;
using SignLanguageAPI.Models;
using System.Security.Cryptography;
using Microsoft.AspNetCore.Cryptography.KeyDerivation;
using System.Text;
using System;
using System.Linq;
using System.Net;
using System.Net.Mail;

namespace SignLanguageAPI.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class AuthController : ControllerBase
    {
        private readonly AppDbContext _context;

        // ✅ for reading SMTP + PublicBaseUrl from appsettings.json
        private readonly IConfiguration _config;

        public AuthController(AppDbContext context, IConfiguration config)
        {
            _context = context;
            _config = config;
        }

        // =========================
        // ✅ LOGIN
        // POST: /api/Auth/login
        // =========================
        [HttpPost("login")]
        public IActionResult Login([FromBody] LoginRequest model)
        {
            if (model == null)
                return BadRequest(new { message = "Invalid request" });

            if (string.IsNullOrWhiteSpace(model.Email))
                return BadRequest(new { message = "Email is required" });

            if (string.IsNullOrWhiteSpace(model.Password))
                return BadRequest(new { message = "Password is required" });

            var email = model.Email.Trim();
            var user = _context.users.FirstOrDefault(u => u.email == email);
            if (user == null)
                return BadRequest(new { message = "Invalid email or password" });

            if (string.IsNullOrWhiteSpace(user.password_salt))
                return BadRequest(new { message = "Password salt missing for this user. Please reset password or re-signup." });

            byte[] saltBytes;
            try
            {
                saltBytes = Convert.FromBase64String(user.password_salt);
            }
            catch
            {
                return BadRequest(new { message = "Invalid password salt format in database." });
            }

            string hashedInput = Convert.ToBase64String(
                KeyDerivation.Pbkdf2(
                    password: model.Password,
                    salt: saltBytes,
                    prf: KeyDerivationPrf.HMACSHA256,
                    iterationCount: 10000,
                    numBytesRequested: 256 / 8
                )
            );

            if (hashedInput != user.password_hash)
                return BadRequest(new { message = "Invalid email or password" });

            return Ok(new
            {
                message = "Login successful",
                user_id = user.user_id,
                full_name = user.full_name,
                email = user.email
            });
        }

        // =========================
        // ✅ SIGNUP
        // POST: /api/Auth/signup
        // =========================
        [HttpPost("signup")]
        public IActionResult Signup([FromBody] users user)
        {
            user.user_id = Guid.Empty;

            if (string.IsNullOrWhiteSpace(user.email))
                return BadRequest(new { message = "Email is required" });

            if (string.IsNullOrWhiteSpace(user.full_name))
                return BadRequest(new { message = "Full name is required" });

            if (string.IsNullOrWhiteSpace(user.password_hash))
                return BadRequest(new { message = "Password is required" });

            user.email = user.email.Trim();

            if (_context.users.Any(u => u.email == user.email))
                return BadRequest(new { message = "Email already registered" });

            byte[] salt = new byte[128 / 8];
            using (var rng = RandomNumberGenerator.Create())
            {
                rng.GetBytes(salt);
            }

            user.password_salt = Convert.ToBase64String(salt);

            string hashed = Convert.ToBase64String(
                KeyDerivation.Pbkdf2(
                    password: user.password_hash,
                    salt: salt,
                    prf: KeyDerivationPrf.HMACSHA256,
                    iterationCount: 10000,
                    numBytesRequested: 256 / 8
                )
            );

            user.password_hash = hashed;

            if (!string.IsNullOrWhiteSpace(user.security_question) &&
                !string.IsNullOrWhiteSpace(user.security_answer_hash))
            {
                string hashedAnswer = Convert.ToBase64String(
                    KeyDerivation.Pbkdf2(
                        password: user.security_answer_hash,
                        salt: Encoding.UTF8.GetBytes(user.email),
                        prf: KeyDerivationPrf.HMACSHA256,
                        iterationCount: 10000,
                        numBytesRequested: 256 / 8
                    )
                );

                user.security_answer_hash = hashedAnswer;
            }

            _context.users.Add(user);
            _context.SaveChanges();

            return Ok(new
            {
                message = "User registered successfully",
                user_id = user.user_id,
                full_name = user.full_name,
                email = user.email
            });
        }

        [HttpPut("edit-profile/{id}")]
        public IActionResult EditProfile(Guid id, [FromBody] EditProfileDto model)
        {
            var user = _context.users.Find(id);
            if (user == null) return NotFound(new { message = "User not found" });

            var newEmail = (model.email ?? "").Trim();

            user.full_name = model.full_name;
            user.email = newEmail;

            var emailExists = _context.users.Any(u => u.email == newEmail && u.user_id != id);
            if (emailExists)
                return BadRequest(new { message = "Email already exists" });

            _context.SaveChanges();

            return Ok(new
            {
                message = "Profile updated",
                user_id = user.user_id,
                full_name = user.full_name,
                email = user.email
            });
        }

        [HttpDelete("delete-profile/{id}")]
        public IActionResult DeleteProfile(Guid id)
        {
            var user = _context.users.Find(id);
            if (user == null) return NotFound(new { message = "User not found" });

            _context.users.Remove(user);
            _context.SaveChanges();

            return Ok(new { message = "Profile deleted successfully" });
        }

        [HttpGet("users")]
        public IActionResult GetUsers()
        {
            var users = _context.users
                .OrderBy(u => u.full_name)
                .Select(u => new
                {
                    user_id = u.user_id,
                    full_name = u.full_name,
                    email = u.email
                })
                .ToList();

            return Ok(users);
        }

        // =========================
        // ✅ FORGET PASSWORD QUESTION
        // POST: /api/Auth/forget-password-question
        // =========================
        [HttpPost("forget-password-question")]
        public IActionResult GetSecurityQuestion([FromBody] string email)
        {
            var user = _context.users.FirstOrDefault(u => u.email == (email ?? "").Trim());
            if (user == null)
                return BadRequest(new { message = "Email not found" });

            return Ok(new
            {
                security_question = user.security_question
            });
        }

        // =========================
        // ✅ VERIFY SECURITY ANSWER
        // POST: /api/Auth/verify-security-answer
        // =========================
        [HttpPost("verify-security-answer")]
        public IActionResult VerifyAnswer([FromBody] VerifyAnswerDto model)
        {
            var user = _context.users.FirstOrDefault(u => u.email == (model.Email ?? "").Trim());
            if (user == null)
                return BadRequest(new { message = "User not found" });

            if (string.IsNullOrWhiteSpace(user.security_answer_hash))
                return BadRequest(new { message = "Security answer not set for this user" });

            string hashedInput = Convert.ToBase64String(
                KeyDerivation.Pbkdf2(
                    password: model.Answer,
                    salt: Encoding.UTF8.GetBytes(user.email),
                    prf: KeyDerivationPrf.HMACSHA256,
                    iterationCount: 10000,
                    numBytesRequested: 256 / 8
                )
            );

            if (hashedInput != user.security_answer_hash)
                return BadRequest(new { message = "Wrong answer" });

            return Ok(new { message = "Answer verified" });
        }

        // =========================
        // ✅ RESET PASSWORD (OLD - security answer based)
        // POST: /api/Auth/reset-password
        // =========================
        [HttpPost("reset-password")]
        public IActionResult ResetPassword([FromBody] ResetPasswordDto model)
        {
            var user = _context.users.FirstOrDefault(u => u.email == (model.Email ?? "").Trim());
            if (user == null)
                return BadRequest(new { message = "User not found" });

            if (string.IsNullOrWhiteSpace(model.NewPassword))
                return BadRequest(new { message = "NewPassword is required" });

            byte[] salt = RandomNumberGenerator.GetBytes(128 / 8);
            user.password_salt = Convert.ToBase64String(salt);

            user.password_hash = Convert.ToBase64String(
                KeyDerivation.Pbkdf2(
                    password: model.NewPassword,
                    salt: salt,
                    prf: KeyDerivationPrf.HMACSHA256,
                    iterationCount: 10000,
                    numBytesRequested: 256 / 8
                )
            );

            _context.SaveChanges();

            return Ok(new { message = "Password reset successful" });
        }

        // =========================================================
        // ✅ NEW FLOW (EMAIL TOKEN) - API 1
        // POST: /api/Auth/request-password-reset
        // =========================================================
        [HttpPost("request-password-reset")]
        public IActionResult RequestPasswordReset([FromBody] RequestPasswordResetDto model)
        {
            // ✅ Always return generic message (security)
            if (model == null || string.IsNullOrWhiteSpace(model.Email))
                return Ok(new { message = "If the email exists, a reset link has been sent." });

            var email = model.Email.Trim();
            var user = _context.users.FirstOrDefault(u => u.email == email);

            if (user != null)
            {
                // ✅ Optional: Purane unused tokens cleanup (same user ke liye)
                var oldTokens = _context.password_reset_tokens
                    .Where(t => t.user_id == user.user_id && t.is_used == false)
                    .ToList();

                if (oldTokens.Count > 0)
                {
                    _context.password_reset_tokens.RemoveRange(oldTokens);
                    _context.SaveChanges();
                }

                var token = GenerateSecureToken();

                var reset = new PasswordResetToken
                {
                    user_id = user.user_id,
                    token = token,
                    expires_at = DateTime.UtcNow.AddMinutes(10), // ✅ 10 min better
                    is_used = false
                };

                _context.password_reset_tokens.Add(reset);
                _context.SaveChanges();

                // ✅ IMPORTANT: PUBLIC HTTPS LINK (clickable in Gmail/WhatsApp)
                // Put your ngrok URL here in appsettings.json: "PublicBaseUrl": "https://xxxx.ngrok-free.dev"
                var publicBaseUrl = (_config["PublicBaseUrl"] ?? "").Trim().TrimEnd('/');

                string resetLink;
                if (!string.IsNullOrWhiteSpace(publicBaseUrl))
                {
                    resetLink = $"{publicBaseUrl}/api/Auth/reset?token={Uri.EscapeDataString(token)}";
                }
                else
                {
                    // fallback (clickable issue ho sakta hai)
                    resetLink = $"signassist://reset?token={Uri.EscapeDataString(token)}";
                }

                try
                {
                    SendResetEmail(email, resetLink);
                }
                catch
                {
                    // Don't reveal SMTP errors to client
                }
            }

            return Ok(new { message = "If the email exists, a reset link has been sent." });
        }

        // =========================================================
        // ✅ CLICKABLE LANDING PAGE (Gmail/WhatsApp)
        // GET: /api/Auth/reset?token=...
        // =========================================================
        [HttpGet("reset")]
        public ContentResult ResetLanding([FromQuery] string token)
        {
            var safeToken = token ?? "";
            var deepLink = $"signassist://reset?token={Uri.EscapeDataString(safeToken)}";

            var html = $@"
<!DOCTYPE html>
<html>
<head>
  <meta name='viewport' content='width=device-width, initial-scale=1' />
  <title>Reset Password</title>
  <style>
    body {{ font-family: Arial, sans-serif; padding: 24px; }}
    .btn {{
      display:inline-block; padding:12px 16px; background:#2196F3; color:#fff;
      text-decoration:none; border-radius:8px; font-weight:bold;
    }}
    .muted {{ color:#666; margin-top:14px; }}
    .box {{ max-width: 520px; margin: 0 auto; }}
  </style>
</head>
<body>
  <div class='box'>
    <h2>Reset your password</h2>
    <p>Tap below to open SignAssist and reset your password.</p>
    <a class='btn' href='{deepLink}'>Open SignAssist App</a>
    <p class='muted'>If nothing happens, make sure the app is installed.</p>

    <script>
      setTimeout(function() {{
        window.location.href = '{deepLink}';
      }}, 600);
    </script>
  </div>
</body>
</html>";

            return new ContentResult
            {
                Content = html,
                ContentType = "text/html",
                StatusCode = 200
            };
        }

        // =========================================================
        // ✅ NEW FLOW (EMAIL TOKEN) - API 2
        // POST: /api/Auth/confirm-password-reset
        // =========================================================
        [HttpPost("confirm-password-reset")]
        public IActionResult ConfirmPasswordReset([FromBody] ConfirmPasswordResetDto model)
        {
            if (model == null)
                return BadRequest(new { message = "Invalid request" });

            if (string.IsNullOrWhiteSpace(model.Token))
                return BadRequest(new { message = "Token is required" });

            if (string.IsNullOrWhiteSpace(model.NewPassword))
                return BadRequest(new { message = "NewPassword is required" });

            if (model.NewPassword != model.ConfirmPassword)
                return BadRequest(new { message = "Passwords do not match" });

            var tokenValue = model.Token.Trim();

            var tokenRow = _context.password_reset_tokens
                .FirstOrDefault(t => t.token == tokenValue && t.is_used == false);

            if (tokenRow == null)
                return BadRequest(new { message = "Invalid or used token" });

            if (DateTime.UtcNow > tokenRow.expires_at)
                return BadRequest(new { message = "Token expired" });

            var user = _context.users.FirstOrDefault(u => u.user_id == tokenRow.user_id);
            if (user == null)
                return BadRequest(new { message = "User not found" });

            byte[] salt = RandomNumberGenerator.GetBytes(128 / 8);
            user.password_salt = Convert.ToBase64String(salt);

            user.password_hash = Convert.ToBase64String(
                KeyDerivation.Pbkdf2(
                    password: model.NewPassword,
                    salt: salt,
                    prf: KeyDerivationPrf.HMACSHA256,
                    iterationCount: 10000,
                    numBytesRequested: 256 / 8
                )
            );

            tokenRow.is_used = true;

            _context.SaveChanges();

            return Ok(new { message = "Password updated successfully" });
        }

        // =========================
        // ✅ Helpers
        // =========================
        private string GenerateSecureToken()
        {
            var bytes = RandomNumberGenerator.GetBytes(32);
            var token = Convert.ToBase64String(bytes);

            // URL safe
            token = token.Replace("+", "-").Replace("/", "_").Replace("=", "");
            return token;
        }

        private void SendResetEmail(string toEmail, string resetLink)
        {
            var host = _config["Smtp:Host"];
            var portStr = _config["Smtp:Port"];
            var username = _config["Smtp:Username"];
            var password = _config["Smtp:Password"];
            var from = _config["Smtp:From"];
            var enableSslStr = _config["Smtp:EnableSsl"];

            if (string.IsNullOrWhiteSpace(host) ||
                string.IsNullOrWhiteSpace(portStr) ||
                string.IsNullOrWhiteSpace(username) ||
                string.IsNullOrWhiteSpace(password) ||
                string.IsNullOrWhiteSpace(from))
            {
                return;
            }

            int port = int.Parse(portStr);
            bool enableSsl = true;
            if (!string.IsNullOrWhiteSpace(enableSslStr))
                bool.TryParse(enableSslStr, out enableSsl);

            var subject = "Reset your password";

            // ✅ Plain text body (clickable because HTTPS)
            var body =
                "You requested a password reset.\n\n" +
                "Open this link to reset your password:\n" +
                resetLink + "\n\n" +
                "This link expires in 10 minutes.\n" +
                "If you didn’t request this, ignore this email.";

            using (var message = new MailMessage(from, toEmail, subject, body))
            using (var client = new SmtpClient(host, port))
            {
                client.EnableSsl = enableSsl;
                client.Credentials = new NetworkCredential(username, password);
                client.Send(message);
            }
        }
    }
}
