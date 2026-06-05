using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SignLanguage.Models
{
    [Table("videos")]
    public class Video
    {
        [Key]
        [Column("video_id")]
        public Guid VideoId { get; set; }

        [Column("user_id")]
        public Guid? UserId { get; set; }   // ON DELETE SET NULL

        [Column("file_url")]
        public string FileUrl { get; set; } = string.Empty;

        [Column("duration_seconds")]
        public int DurationSeconds { get; set; }
    }
}
