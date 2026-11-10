package com.threelinelimited.rentalservice.controller;

import com.threelinelimited.rentalservice.dto.Rental;
import com.threelinelimited.rentalservice.dto.request.RentAssetRequest;
import com.threelinelimited.rentalservice.enums.UserRoleType;
import com.threelinelimited.rentalservice.service.RentalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rentals")
public class RentalController {
    private static final String CUSTOMER_ONLY = "hasAuthority('" + UserRoleType.Code.ROLE_CUSTOMER + "')";
    private static final String CUSTOMER_OR_MERCHANT =
            "hasAnyAuthority('" + UserRoleType.Code.ROLE_CUSTOMER + "','" + UserRoleType.Code.ROLE_MERCHANT + "')";

    private final RentalService rentalService;

    @PostMapping
    @PreAuthorize(CUSTOMER_ONLY)
    public ResponseEntity<Rental> rent(Authentication auth, @Valid @RequestBody RentAssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rentalService.rentAsset(auth.getName(), request));
    }

    @GetMapping
    @PreAuthorize(CUSTOMER_OR_MERCHANT)
    public ResponseEntity<Page<Rental>> myRentals(Authentication auth,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(rentalService.getMyRentals(auth.getName(), page, size));
    }

    @GetMapping("/{reference}")
    @PreAuthorize(CUSTOMER_OR_MERCHANT)
    public ResponseEntity<Rental> get(Authentication auth, @PathVariable String reference) {
        return ResponseEntity.ok(rentalService.getRental(auth.getName(), reference));
    }
}
