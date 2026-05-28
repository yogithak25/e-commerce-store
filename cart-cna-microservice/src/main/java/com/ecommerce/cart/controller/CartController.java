package com.ecommerce.cart.controller;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.cart.model.Cart;
import com.ecommerce.cart.model.CartItem;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@CrossOrigin
@RestController
public class CartController {

    private static final Logger LOG =
        LoggerFactory.getLogger(CartController.class);

    private ReactiveRedisTemplate<String, Cart> redisTemplate;

    private ReactiveValueOperations<String, Cart> cartOps;

    CartController(ReactiveRedisTemplate<String, Cart> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.cartOps = this.redisTemplate.opsForValue();
    }

    @RequestMapping("/")
    public String index() {
        return "{ \"name\": \"Cart API\", \"version\": 1.0.0}";
    }

    @GetMapping("/cart")
    public Flux<Cart> list() {
        return redisTemplate.keys("*")
                .flatMap(cartOps::get);
    }

    @GetMapping("/cart/{customerId}")
    public Mono<Cart> findById(@PathVariable String customerId) {
        return cartOps.get(customerId);
    }

    @PostMapping("/cart")
    public Mono<Boolean> create(@RequestBody Mono<Cart> cartMono) {

        return cartMono.flatMap(newCart -> {

            LOG.info("Incoming cart: {}", newCart);

            return cartOps.get(newCart.getCustomerId())
                .defaultIfEmpty(new Cart())
                .flatMap(existingCart -> {

                    existingCart.setCustomerId(
                        newCart.getCustomerId()
                    );

                    if (existingCart.getItems() == null) {
                        existingCart.setItems(new ArrayList<>());
                    }

                    if (newCart.getItems() != null) {

                        for (CartItem newItem : newCart.getItems()) {

                            boolean itemExists = false;

                            for (CartItem existingItem :
                                 existingCart.getItems()) {

                                if (existingItem.getSku()
                                    .equals(newItem.getSku())) {

                                    existingItem.setQuantity(
                                        existingItem.getQuantity()
                                        + newItem.getQuantity()
                                    );

                                    itemExists = true;
                                    break;
                                }
                            }

                            if (!itemExists) {
                                existingCart.getItems().add(newItem);
                            }
                        }
                    }

                    float total = 0;

                    for (CartItem item : existingCart.getItems()) {
                        total += item.getPrice()
                                 * item.getQuantity();
                    }

                    existingCart.setTotal(total);

                    LOG.info("Saving cart: {}", existingCart);

                    return cartOps.set(
                        existingCart.getCustomerId(),
                        existingCart
                    );
                });
        });
    }
}
