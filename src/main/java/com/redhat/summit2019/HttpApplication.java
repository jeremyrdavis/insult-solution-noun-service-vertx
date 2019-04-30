package com.redhat.summit2019;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.redhat.summit2019.model.Noun;

public class HttpApplication extends AbstractVerticle {

  static final String template = "Hello, %s!";

  private List<Noun> nouns;

  @Override
  public void start(Future<Void> startFuture) {
    Future init = loadNouns().compose(v -> startHttpServer()).setHandler(startFuture.completer());
  }

  private void nounHandler(RoutingContext rc) {

;
    JsonObject response = new JsonObject()
            .put("noun", nouns.get(new Random().nextInt(nouns.size())).getNoun());

    rc.response()
            .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
            .end(response.encodePrettily());
  }

  private void greeting(RoutingContext rc) {
    String name = rc.request().getParam("name");
    if (name == null) {
      name = "World";
    }

    JsonObject response = new JsonObject()
        .put("content", String.format(template, name));

    rc.response()
        .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
        .end(response.encodePrettily());
  }

  private Future<Void> startHttpServer(){
    Future<Void> future = Future.future();

    // Create a router object.
    Router router = Router.router(vertx);

    router.get("/api/greeting").handler(this::greeting);
    router.get("/api/noun").handler(this::nounHandler);
    router.get("/*").handler(StaticHandler.create());

    // Create the HTTP server and pass the "accept" method to the request handler.
    vertx
      .createHttpServer()
      .requestHandler(router)
      .listen(
              // Retrieve the port from the configuration, default to 8080.
              config().getInteger("http.port", 8080), ar -> {
                if (ar.succeeded()) {
                  System.out.println("Server started on port " + ar.result().actualPort());
                  future.complete();
                }else{
                  future.fail(ar.cause());
                }
              });

    return future;
  }

  private Future<Void> loadNouns() {

    if (nouns == null) {
      nouns = new ArrayList<>();
    }

    Future<Void> future = Future.future();

    try {
      InputStream is = this.getClass().getClassLoader().getResourceAsStream("nouns.txt");
      if (is != null) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        reader.lines()
                .forEach(n -> nouns.add(new Noun(n.trim())));
      }
      future.complete();
    } catch (Exception e) {
      e.printStackTrace();
      future.fail(e.getCause());
    }

    return future;
  }
}