package cl.duoc.mssmartrentcheckout.service;

import cl.duoc.mssmartrentcheckout.config.RabbitMQConfig;
import cl.duoc.mssmartrentcheckout.dto.CheckoutRequest;
import cl.duoc.mssmartrentcheckout.dto.OrderCreatedEvent;
import cl.duoc.mssmartrentcheckout.model.Order;
import cl.duoc.mssmartrentcheckout.model.OrderItem;
import cl.duoc.mssmartrentcheckout.repository.OrderRepository;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CheckoutService {

    private final OrderRepository orderRepository;
    private final AmqpTemplate rabbitTemplate;

    public CheckoutService(OrderRepository orderRepository, AmqpTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public Order processCheckout(String userId, CheckoutRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("La orden debe contener al menos un item");
        }

        // Crear la orden
        Order order = new Order(
                userId,
                request.getType(),
                request.getCustomerType(),
                request.getRut(),
                request.getName(),
                request.getPhone(),
                request.getDeliveryType(),
                request.getDeliveryAddress(),
                request.getPaymentMethod()
        );

        // Agregar los items a la orden
        request.getItems().forEach(dto -> {
            OrderItem item = new OrderItem(dto.getMachineId(), dto.getMachineName(), dto.getDailyPrice());
            order.addItem(item);
        });

        // Guardar en base de datos
        Order savedOrder = orderRepository.save(order);

        // Publicar evento en RabbitMQ para ms-smartrent-rentals y ms-smartrent-notify
        publishOrderCreatedEvent(savedOrder, request.getItems());

        return savedOrder;
    }

    private void publishOrderCreatedEvent(Order order, List<CheckoutRequest.ItemDto> items) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getUserId(),
                order.getRut(),
                order.getName(),
                order.getPhone(),
                order.getCustomerType(),
                order.getType(),
                order.getDeliveryType(),
                order.getDeliveryAddress(),
                order.getPaymentMethod(),
                items
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "order.created", event);
    }
}
