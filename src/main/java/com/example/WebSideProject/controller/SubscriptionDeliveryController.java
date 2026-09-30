package com.example.WebSideProject.controller;

import com.example.WebSideProject.dto.DeliveryOverviewDto;
import com.example.WebSideProject.service.SubscriptionDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/delivery")
@RequiredArgsConstructor
public class SubscriptionDeliveryController {

    private final SubscriptionDeliveryService deliveryService;

    @GetMapping
    public ResponseEntity<DeliveryOverviewDto> getOwnDelivery(
            @RequestHeader(value = "X-Coders-User", required = false) String codersUserId
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(deliveryService.getOwnOverview(codersUserId));
    }
}
