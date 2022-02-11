package com.graphql.springgraphqldemo;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@SpringBootApplication
public class SpringGraphqldemoApplication {

  public static void main(String[] args) {
    SpringApplication.run(SpringGraphqldemoApplication.class, args);
  }
}

@Controller
@RestController
class SubjectGraphQLController {

  Map<Integer, Sinks.Many<SubjectEvent>> manyMap = new ConcurrentHashMap<Integer, Sinks.Many<SubjectEvent>>();
  Sinks.Many<SubjectEvent> globalFlux = Sinks
    .many()
    .multicast()
    .<SubjectEvent>onBackpressureBuffer();

  Subjectrepo repo;

  SubjectGraphQLController(Subjectrepo repo) {
    this.repo = repo;
  }

  /*  @SchemaMapping(typeName = "Query", field = "subjects")
  Flux<Subject> subjectStreamS() {
    return repo.findAll();
  } */

  /*   @QueryMapping("subjects")
  Flux<Subject> subjectStreamQ() {
    return repo.findAll();
  } */

  @QueryMapping
  Flux<Subject> subjects() {
    return repo.findAll();
  }

  @QueryMapping
  Flux<Subject> subjectsByCity(@Argument String city) {
    return repo.findByCity(city);
  }

  @MutationMapping
  Mono<Subject> addSubject(@Argument String name, @Argument String city) {
    return repo.save(new Subject(null, name, city));
  }

  @SubscriptionMapping
  Flux<SubjectEvent> subjectEventSubscription(@Argument Integer subjectid) {
    System.out.println("manyMap.get(subjectid) " + manyMap.get(subjectid));
    if (manyMap.get(subjectid) == null) {
      System.out.println("init subscription .. adding socket flux");
      manyMap.put(
        subjectid,
        Sinks.many().multicast().<SubjectEvent>onBackpressureBuffer()
      );
    }
    return manyMap.get(subjectid).asFlux();
  }

  @SubscriptionMapping
  Flux<SubjectEvent> globalEventSubscription() {
    return globalFlux.asFlux();
  }

  @SchemaMapping(typeName = "Subject")
  Flux<SubjectSession> sessions(Subject subject) {
    return Flux
      .range(1, subject.getId())
      .map(
        id ->
          new SubjectSession(
            UUID.randomUUID().toString(),
            subject.getId(),
            "WEB",
            true,
            Instant.now().toEpochMilli(),
            0L
          )
      );
  }

  @GetMapping("/subject/login/{id}")
  public Mono<String> subjectLogin(@PathVariable Integer id) {
    repo
      .findById(id)
      .map(
        subject ->
          new SubjectEvent(
            subject,
            SubjectEventType.LOGIN,
            "WEB",
            Instant.now().toEpochMilli()
          )
      )
      .subscribe(
        se -> {
          if (manyMap.get(id) != null) manyMap.get(id).tryEmitNext(se);
          globalFlux.tryEmitNext(se);
        }
      );
    return Mono.just("Logged in");
  }
}

interface Subjectrepo extends ReactiveCrudRepository<Subject, Integer> {
  Flux<Subject> findByCity(String city);
}

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
class Subject implements Serializable {

  @Id
  private Integer id;

  private String name;
  private String city;
}

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
class SubjectSession implements Serializable {

  @Id
  private String sessionid;

  private Integer subjectid;
  private String channel;
  private Boolean active;
  private Long starttime;
  private Long endtime;
}

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
class SubjectEvent implements Serializable {

  private Subject subject;
  private SubjectEventType type;
  private String channel;
  private Long starttime;
}

enum SubjectEventType {
  LOGIN,
  LOGOFF,
}
