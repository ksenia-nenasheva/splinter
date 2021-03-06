package ru.yandex.autoschool.splinter.resources;

import org.glassfish.jersey.server.mvc.Template;
import ru.yandex.autoschool.splinter.config.ApplicationConfig;
import ru.yandex.autoschool.splinter.models.Post;
import ru.yandex.autoschool.splinter.models.User;
import ru.yandex.autoschool.splinter.utils.freemarker.MarkdownMethod;
import ru.yandex.autoschool.splinter.view.ViewData;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Etki {@literal <etki@etki.name>}
 * @version %I%, %G%
 * @since 1.0
 */
@Path("/")
@Produces(MediaType.TEXT_HTML)
public class IndexResource extends BaseResource {

    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse response;
    @Context
    SecurityContext securityContext;

    @GET
    @Path("/")
    @Template(name = "/templates/index/index.ftl")
    public ViewData indexAction() {
        ViewData.set("markdownize", new MarkdownMethod());
        ViewData.set("model", Post.findAll().limit(ApplicationConfig.POSTS_PER_PAGE).orderBy("created_at desc"));
        return ViewData;
    }

    @GET
    @Path("/about")
    @Template(name = "/templates/index/about.ftl")
    public ViewData aboutAction() {
        return ViewData;
    }

    @GET
    @Path("/profile")
    @Template(name = "/templates/user/profile.ftl")
    public ViewData showProfileAction() throws IOException {
        HttpSession session = request.getSession(true);

        if (session.getAttribute("userId") == null) {
            response.sendRedirect("/signin");
        }

        ViewData.set("profile", (User) securityContext.getUserPrincipal());

        return ViewData;
    }

}
