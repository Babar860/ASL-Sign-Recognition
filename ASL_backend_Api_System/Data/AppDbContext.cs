using Microsoft.EntityFrameworkCore;
using SignLanguage.Models;
using SignLanguageAPI.Models;

namespace SignLanguageAPI.Data
{
    public class AppDbContext : DbContext
    {
        public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

        public DbSet<users> users { get; set; }

        public DbSet<Video> Videos { get; set; }

        // ✅ NEW: Password Reset Tokens table
        public DbSet<PasswordResetToken> password_reset_tokens { get; set; }

        public DbSet<TranslationHistory> TranslationHistory { get; set; }

        public DbSet<UserFeedback> UserFeedback { get; set; }
    }
}
