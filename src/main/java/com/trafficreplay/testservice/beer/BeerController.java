package com.trafficreplay.testservice.beer;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/beers")
public class BeerController {

    private final BeerService beerService;

    public BeerController(BeerService beerService) {
        this.beerService = beerService;
    }

    @PostMapping
    public ResponseEntity<Beer> create(@Valid @RequestBody CreateBeerRequest request) {
        Beer created = beerService.create(request);
        return ResponseEntity.created(URI.create("/api/beers/" + created.id())).body(created);
    }

    @GetMapping
    public List<Beer> findAll() {
        return beerService.findAll();
    }

    @GetMapping("/{id}")
    public Beer get(@PathVariable String id) {
        return beerService.get(id);
    }

    @PatchMapping("/{id}")
    public Beer patch(@PathVariable String id, @Valid @RequestBody PatchBeerRequest request) {
        return beerService.patch(id, request);
    }

    @ExceptionHandler(BeerNotFoundException.class)
    public ResponseEntity<String> handleNotFound(BeerNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}
