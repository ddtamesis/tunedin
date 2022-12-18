package server.handlers;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.util.Map;
import database.UserDatabase;
import server.ErrBadJsonResponse;
import spark.Request;
import spark.Response;
import spark.Route;
import user.User;

public class GetUserHandler implements Route {

  UserDatabase database;

  public GetUserHandler(UserDatabase database) {
    this.database = database;
  }

  @Override
  public Object handle(Request request, Response response) throws Exception {
    try {
      String userId = request.queryParams("id");
      User user = this.database.getUser(userId);
      System.out.println("User retrieved.");
      return new GetUserSuccessResponse(user).serialize();
    } catch (Exception e) {
      return new ErrBadJsonResponse();
    }
  }

  public record GetUserSuccessResponse(String result, User user) {

    public GetUserSuccessResponse(User user) {
      this("success", user);
    }

    public String serialize() {
      try {
        Moshi moshi = new Moshi.Builder().build();

        JsonAdapter<GetUserSuccessResponse> adapter = moshi.adapter(GetUserSuccessResponse.class);
        return adapter.toJson(this);
      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      }
    }
  }
}
