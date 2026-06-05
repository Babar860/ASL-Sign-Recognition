using Microsoft.AspNetCore.Mvc;

namespace SignLanguageAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class LearningController : ControllerBase
    {
        private readonly IConfiguration _config;

        public LearningController(IConfiguration config)
        {
            _config = config;
        }

        [HttpGet("alphabet")]
        public IActionResult Alphabet()
        {
            var baseUrl = GetAslPublicBaseUrl();
            var items = Enumerable.Range('A', 26)
                .Select(code => BuildAlphabetItem(((char)code).ToString(), baseUrl))
                .ToList();

            return Ok(items);
        }

        [HttpGet("alphabet/{letter}")]
        public IActionResult AlphabetLetter(string letter)
        {
            var value = (letter ?? "").Trim().ToUpperInvariant();
            if (value.Length != 1 || value[0] < 'A' || value[0] > 'Z')
                return BadRequest(new { message = "Letter must be A-Z" });

            return Ok(BuildAlphabetItem(value, GetAslPublicBaseUrl()));
        }

        private object BuildAlphabetItem(string letter, string baseUrl)
        {
            return new
            {
                letter,
                label = $"Sign for {letter}",
                imageUrl = $"{baseUrl}/assets/text_to_sign/signs/{letter}.png"
            };
        }

        private string GetAslPublicBaseUrl()
        {
            return (_config["AslPublicBaseUrl"] ?? "http://192.168.0.33:8000").TrimEnd('/');
        }
    }
}
