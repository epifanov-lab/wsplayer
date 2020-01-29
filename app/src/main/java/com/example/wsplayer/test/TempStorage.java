package com.example.wsplayer.test;

import org.reactivestreams.Publisher;

import java.util.HashMap;
import java.util.Map;

import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * @author Konstantin Epifanov
 * @since 29.01.2020
 */
public class TempStorage {
  private Map<String, byte[]> mMap;
  private DirectProcessor<String> mChanges;

  public TempStorage() {
    mMap= new HashMap<>();
    mChanges= DirectProcessor.create();
  }

  Publisher<Void> put(String key, byte... value){
    mMap.put(key, value);
    mChanges.sink().next(key);
    return Mono.empty();
  }

  Publisher<byte[]> get(boolean first, String key){
    return Flux.merge(first ? Mono.just(mMap.computeIfAbsent(key, (k) -> new byte[0])) : Mono.empty(),
                      mChanges
                        .filter((s -> s.equals(key)))
                        .map((k) -> mMap.get(k)));
  }

}
