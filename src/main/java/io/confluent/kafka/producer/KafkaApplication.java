package io.confluent.kafka.producer;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.stream.Stream;

import io.confluent.demo.Movie;
import io.confluent.demo.Rating;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@SpringBootApplication
@EnableKafka
@RequiredArgsConstructor
public class KafkaApplication {

  public static void main(String[] args) {
    SpringApplication.run(KafkaApplication.class, args);
  }

  @Bean
  NewTopic movies() {
    return new NewTopic("movies", 12, (short) 3);
  }

  @Bean
  NewTopic ratings() {
    return new NewTopic("ratings", 12, (short) 3);
  }

}


@Component
@RequiredArgsConstructor
@Log4j2
class Producer {

  private static final String MOVIES_TOPIC = "movies";
  private static final String RATINGS_TOPIC = "ratings";
  private final KafkaTemplate<Object, Object> producer;

  @Value("classpath:movies.dat")
  Resource moviesFile;

  @EventListener(ApplicationReadyEvent.class)
  public void process() {

    try (Stream<String> stream = Files.lines(Paths.get(moviesFile.getURI()))) {

      stream.forEach(s -> {
        final Movie movie = Parser.parseMovie(s);
        producer.send(MOVIES_TOPIC, movie.getMovieId(), movie);
      });
    } catch (IOException e) {
      e.printStackTrace();
    }

    Random ran = new Random();
    while (true) {
      // minimum + rn.nextInt(maxValue - minvalue + 1)
      int movieId = ran.nextInt(920) + 1;
      int rating = 5 + ran.nextInt(6);

      final Rating rat = new Rating((long) movieId, (double) rating);
      log.debug(rat.toString());
      this.producer.send(RATINGS_TOPIC, rat.getMovieId(), rat);
    }

  }

}