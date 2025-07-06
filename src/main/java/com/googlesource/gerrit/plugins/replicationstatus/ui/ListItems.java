src/main/java/com/googlesource/gerrit/plugins/replicationstatus/ui/ListItems.javapackage com.googlesource.gerrit.plugins.replicationstatus.ui;

import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.project.ProjectResource;
import java.util.List;
import javax.inject.Inject;

public class ListItems implements RestReadView<ProjectResource> {

  @Inject
  public ListItems() {}


  @Override
  public Response<?> apply(ProjectResource resource) throws AuthException, BadRequestException, ResourceConflictException, Exception {
    return Response.ok(List.of(new ItemInfo(1, "one")));
  }

  public static class ItemInfo {
    public long id;
    public String item;

    public ItemInfo(long id, String item) {
      this.id = id;
      this.item = item;
    }
  }
}
