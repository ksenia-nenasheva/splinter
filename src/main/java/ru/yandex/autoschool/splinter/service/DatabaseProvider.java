package ru.yandex.autoschool.splinter.service;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.FileSystemResourceAccessor;
import org.apache.log4j.BasicConfigurator;
import org.h2.jdbcx.JdbcDataSource;
import org.javalite.activejdbc.Base;
import ru.yandex.autoschool.splinter.config.FilterPriorities;
import ru.yandex.autoschool.splinter.config.ServerConfig;
import ru.yandex.autoschool.splinter.config.database.AbstractDatabaseConfig.DRIVER;
import ru.yandex.autoschool.splinter.config.database.DatabaseConfig;

import javax.annotation.Priority;
import javax.sql.DataSource;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

import static ru.yandex.autoschool.splinter.SplinterApplication.LOGGER;

import static java.lang.String.format;


/**
 * @author eroshenkoam
 * @version %I%, %G%
 * @since 1.0
 */
@Priority(FilterPriorities.DBCONNECTION)
@Provider
@SuppressWarnings("unused")
@edu.umd.cs.findbugs.annotations.SuppressWarnings(
        value = "org.sonar.java.checks.SystemExitCalledCheck",
        justification = "Halts whole application in case database is not available and there's no point in going further"
)
public class DatabaseProvider implements ContainerRequestFilter {
    private static DatabaseConfig config;
    private static String dbUrl;
    private static DRIVER dbDriver;
    private static String dbUser;
    private static String dbPassword;

    static {
        BasicConfigurator.configure();
        ServerConfig serverConfig = new ServerConfig();
        config = serverConfig.getDatabaseConfig();
        try {
            dbUrl = config.getUrl();
            dbDriver = config.getDriver();
            dbUser = config.getUser();
            dbPassword = config.getPassword();

            LOGGER.info(format("Starting embedded database with url '%s' ...", dbUrl));
            String changeLogPath = getChangeLogLocation();
            LOGGER.info(format("Using `%s` as changelog path", changeLogPath));
            Liquibase liquibaseMigrationManager = new Liquibase(
                    changeLogPath, new FileSystemResourceAccessor(), getLiquibaseConnection()
            );
            liquibaseMigrationManager.update("");
            String fixturesPath = getFixturesLocation();
            Liquibase liquibaseFixtureImporter = new Liquibase(
                    fixturesPath, new FileSystemResourceAccessor(), getLiquibaseConnection()
            );
            liquibaseFixtureImporter.update("");
            openConnection();
        } catch (Exception e) {
            LOGGER.error("Failed to start embedded database", e);
            System.exit(-1);
        }
    }
    private static DataSource getDataSource(DRIVER driver, String url, String user, String password) {
        DataSource dataSource;
        switch (driver) {
            case H2:
                JdbcDataSource h2DataSource = new JdbcDataSource();
                h2DataSource.setUrl(url);
                h2DataSource.setUser(user);
                h2DataSource.setPassword(password);
                return h2DataSource;
            case MYSQL:
                MysqlDataSource mysqlDataSource = new MysqlDataSource();
                mysqlDataSource.setURL(url);
                mysqlDataSource.setUser(user);
                mysqlDataSource.setPassword(password);
                return mysqlDataSource;
            default:
                throw new IllegalArgumentException("Unknown database driver");
        }
    }

    public static void openConnection() {
        if (!Base.hasConnection()) {
            String user = dbUser != null ? dbUser : "";
            String password = dbPassword != null ? dbPassword : "";
            switch (dbDriver) {
                case MYSQL:
                    Base.open(com.mysql.jdbc.Driver.class.getName(), dbUrl, user, password);
                    break;
                case H2:
                    Base.open(org.h2.Driver.class.getName(), dbUrl, user, password);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown database driver");
            }
        }
    }

    private static String getChangeLogLocation() {
        URL url = DatabaseProvider.class.getClassLoader().getResource("/db/migration/changelog.xml");
        return (url == null) ? null : url.getFile();
    }

    private static String getFixturesLocation() {
        URL url = DatabaseProvider.class.getClassLoader().getResource("/db/fixtures/fixtures.xml");
        return (url == null) ? null : url.getFile();
    }

    private static JdbcConnection getLiquibaseConnection() throws SQLException {
        DataSource dataSource = getDataSource(dbDriver, dbUrl, dbUser, dbPassword);
        return new JdbcConnection(dataSource.getConnection());
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        openConnection();
    }
}
