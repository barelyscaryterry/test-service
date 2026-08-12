package com.trafficreplay.testservice.beer;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BeerService {

    private final BeerRepository beerRepository;

    public BeerService(BeerRepository beerRepository) {
        this.beerRepository = beerRepository;
    }

    public Beer create(CreateBeerRequest request) {
        Instant now = Instant.now();
        Beer beer = new Beer(
                UUID.randomUUID().toString(),
                request.name(),
                request.brewery(),
                request.style(),
                request.abv(),
                now,
                now);
        return beerRepository.put(beer);
    }

    public Beer get(String id) {
        return beerRepository.get(id);
    }

    public List<Beer> findAll() {
        return beerRepository.findAll();
    }

    public Beer patch(String id, PatchBeerRequest request) {
        Beer existing = beerRepository.get(id);
        Beer patched = new Beer(
                existing.id(),
                request.name() != null ? request.name() : existing.name(),
                request.brewery() != null ? request.brewery() : existing.brewery(),
                request.style() != null ? request.style() : existing.style(),
                request.abv() != null ? request.abv() : existing.abv(),
                existing.createdAt(),
                Instant.now());
        return beerRepository.put(patched);
    }
}
