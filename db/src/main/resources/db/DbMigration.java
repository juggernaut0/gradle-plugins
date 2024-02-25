import org.flywaydb.core.Flyway;

public class DbMigration {
    public static void runMigrations(String url, String username, String password) {
        Flyway.configure()
                .dataSource(url, username, password)
                .load()
                .migrate();
    }

    public static void main(String[] args) {
        String url = args[0];
        String username = args[1];
        String password = args[2];
        runMigrations(url, username, password);
    }
}
