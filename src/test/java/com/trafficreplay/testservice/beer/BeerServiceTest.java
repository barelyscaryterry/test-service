package com.trafficreplay.testservice.beer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeerServiceTest {

    @Mock
    private BeerRepository beerRepository;

    private BeerService beerService;

    @BeforeEach
    void setUp() {
        beerService = new BeerService(beerRepository);
    }

    @Test
    void createAssignsIdAndTimestampsThenPersists() {
        CreateBeerRequest request = new CreateBeerRequest("Pliny the Elder", "Russian River", "IPA", new BigDecimal("8.0"));
        when(beerRepository.put(any(Beer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Beer created = beerService.create(request);

        assertThat(created.id()).isNotBlank();
        assertThat(created.name()).isEqualTo("Pliny the Elder");
        assertThat(created.brewery()).isEqualTo("Russian River");
        assertThat(created.style()).isEqualTo("IPA");
        assertThat(created.abv()).isEqualByComparingTo("8.0");
        assertThat(created.createdAt()).isEqualTo(created.updatedAt());

        ArgumentCaptor<Beer> captor = ArgumentCaptor.forClass(Beer.class);
        verify(beerRepository).put(captor.capture());
        assertThat(captor.getValue()).isEqualTo(created);
    }

    @Test
    void getDelegatesToRepository() {
        Beer beer = beerFixture("1");
        when(beerRepository.get("1")).thenReturn(beer);

        Beer result = beerService.get("1");

        assertThat(result).isEqualTo(beer);
    }

    @Test
    void getPropagatesNotFoundFromRepository() {
        when(beerRepository.get("missing")).thenThrow(new BeerNotFoundException("missing"));

        org.junit.jupiter.api.Assertions.assertThrows(
                BeerNotFoundException.class,
                () -> beerService.get("missing"));
    }

    @Test
    void findAllDelegatesToRepository() {
        List<Beer> beers = List.of(beerFixture("1"), beerFixture("2"));
        when(beerRepository.findAll()).thenReturn(beers);

        List<Beer> result = beerService.findAll();

        assertThat(result).isEqualTo(beers);
    }

    @Test
    void patchAppliesOnlyNonNullFieldsAndKeepsCreatedAt() {
        Beer existing = beerFixture("1");
        PatchBeerRequest request = new PatchBeerRequest("New Name", null, null, null);
        when(beerRepository.get("1")).thenReturn(existing);
        when(beerRepository.put(any(Beer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Beer patched = beerService.patch("1", request);

        assertThat(patched.id()).isEqualTo(existing.id());
        assertThat(patched.name()).isEqualTo("New Name");
        assertThat(patched.brewery()).isEqualTo(existing.brewery());
        assertThat(patched.style()).isEqualTo(existing.style());
        assertThat(patched.abv()).isEqualByComparingTo(existing.abv());
        assertThat(patched.createdAt()).isEqualTo(existing.createdAt());
        assertThat(patched.updatedAt()).isAfterOrEqualTo(existing.updatedAt());
    }

    @Test
    void patchThrowsWhenBeerDoesNotExist() {
        PatchBeerRequest request = new PatchBeerRequest("New Name", null, null, null);
        when(beerRepository.get("missing")).thenThrow(new BeerNotFoundException("missing"));

        org.junit.jupiter.api.Assertions.assertThrows(
                BeerNotFoundException.class,
                () -> beerService.patch("missing", request));
    }

    private static Beer beerFixture(String id) {
        Instant now = Instant.now();
        return new Beer(id, "Pliny the Elder", "Russian River", "IPA", new BigDecimal("8.0"), now, now);
    }
}
