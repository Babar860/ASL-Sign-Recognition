
using SignLanguageAPI.Data;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseNpgsql(builder.Configuration.GetConnectionString("DefaultConnection"))
);

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();
app.Urls.Add("http://0.0.0.0:5140");

using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    db.Database.Migrate();
    db.Database.ExecuteSqlRaw("""
        CREATE TABLE IF NOT EXISTS translation_history (
            history_id uuid PRIMARY KEY,
            user_id uuid NOT NULL,
            sentence text NOT NULL,
            words text[] NOT NULL,
            translation_type text NOT NULL,
            created_at timestamp with time zone NOT NULL
        );
    """);
    db.Database.ExecuteSqlRaw("""
        CREATE TABLE IF NOT EXISTS user_feedback (
            feedback_id uuid PRIMARY KEY,
            user_id uuid NOT NULL,
            feedbacks text[] NOT NULL,
            created_at timestamp with time zone NOT NULL,
            updated_at timestamp with time zone NOT NULL
        );
    """);
}

app.UseSwagger();
app.UseSwaggerUI();
app.UseHttpsRedirection();
app.UseAuthorization();
app.MapControllers();
app.Run();
