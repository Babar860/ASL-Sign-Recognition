using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace SignLanguage.Migrations
{
    /// <inheritdoc />
    public partial class AddPasswordSaltToUsers : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "password_salt",
                table: "users",
                type: "text",
                nullable: false,
                defaultValue: "");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "password_salt",
                table: "users");
        }
    }
}
