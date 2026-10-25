package demo;

import dev.tracekit.core.Traced;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SpringBootApplication
@RestController
public class DemoApplication {
    private final PricingService pricing;
    private final CheckoutService checkout;

    public DemoApplication(PricingService pricing, CheckoutService checkout) { this.pricing = pricing; this.checkout = checkout; }

    public static void main(String[] args) { SpringApplication.run(DemoApplication.class, args); }

    // try: curl 'localhost:8080/checkout?user=bob&password=hunter2'
    @GetMapping("/checkout")
    public double checkout(@RequestParam String user, @RequestParam String password) {
        return checkout.checkout(user, password, List.of("book", "pen", "lamp"));
    }

    @Service 
    public static class PricingService {
        @Traced 
        public double price(String item) { return item.length() * 1.5; }
    }

    @Service public static class CheckoutService {
        private final PricingService pricing;
        public CheckoutService(PricingService pricing) { this.pricing = pricing; }

        @Traced
        public double checkout(String user, String password, List<String> items) {
            return items.stream().mapToDouble(pricing::price).sum();
        }
    }
}
