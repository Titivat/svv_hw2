package th.ac.kmitl.se;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.List;

import static org.mockito.Mockito.*;

class OrderTest {
    OrderDB orderDB;
    ProductDB productDB;
    PaymentService paymentService;
    ShippingService shippingService;
    Order order;

    @BeforeEach
    void setUp() {
        orderDB = mock(OrderDB.class);
        productDB = mock(ProductDB.class);
        paymentService = mock(PaymentService.class);
        shippingService = mock(ShippingService.class);
        order = Mockito.spy(new Order(orderDB, productDB, paymentService, shippingService));
    }

    @Test
    void testPlacingOrder() {
        Address mockAddress = new Address("name", "line1", "line2", "district", "city", "postcode");
        when(orderDB.getOrderID()).thenReturn(123);

        order.place("John", "Appl Watch", 2, mockAddress);
        assertEquals(Order.Status.PLACED, order.getStatus());
        verify(orderDB).update(order);
    }

    @Test
    void testCancelingUnpaidOrder() {
        Address mockAddress = new Address("name", "line1", "line2", "district", "city", "postcode");
        when(orderDB.getOrderID()).thenReturn(1);

        order.place("John", "Appl Watch", 2, mockAddress);
        assertEquals(Order.Status.PLACED, order.getStatus());
        verify(orderDB).update(order);

        order.cancel();
        assertEquals(Order.Status.CANCELED, order.getStatus());
        verify(orderDB, times(2)).update(order);
    }

    @Test
    void testPaymentSuccess() {
        Address mockAddress = new Address("name", "line1", "line2", "district", "city", "postcode");
        when(orderDB.getOrderID()).thenReturn(1);

        order.place("John", "Appl Watch", 2, mockAddress);
        assertEquals(Order.Status.PLACED, order.getStatus());

        //payment
        Card card = new Card("123", "JohnCena", 2, 2023);
        // pho getTotalCost to work
        when(productDB.getPrice("Appl Watch")).thenReturn(1500.0F);
        when(productDB.getWeight("Appl Watch")).thenReturn(350.0F);
        when(shippingService.getPrice(mockAddress, 700.0F)).thenReturn(50.0F);
        assertEquals(order.getTotalCost(), 3050.0F);

        //check for Status.PAYMENT_CHECK
        order.pay(card);
        assertEquals(Order.Status.PAYMENT_CHECK, order.getStatus());
        verify(orderDB, times(2)).update(order);

        // trigger .onSuccess()
        ArgumentCaptor<PaymentCallback> callbackCaptor = ArgumentCaptor.forClass(PaymentCallback.class);

        verify(paymentService).pay(any(Card.class), anyFloat(), callbackCaptor.capture());
        callbackCaptor.getValue().onSuccess("123");

        assertEquals(Order.Status.PAID, order.getStatus());
        assertEquals(order.paymentConfirmCode, "123");
        verify(orderDB, times(3)).update(order);
    }

    @Test
    void testPaymentError() {
        Address mockAddress = new Address("name", "line1", "line2", "district", "city", "postcode");
        when(orderDB.getOrderID()).thenReturn(1);

        order.place("John", "Appl Watch", 2, mockAddress);
        assertEquals(Order.Status.PLACED, order.getStatus());

        //payment
        Card card = new Card("123", "JohnCena", 2, 2023);
        // pho getTotalCost to work
        when(productDB.getPrice("Appl Watch")).thenReturn(1500.0F);
        when(productDB.getWeight("Appl Watch")).thenReturn(350.0F);
        when(shippingService.getPrice(mockAddress, 700.0F)).thenReturn(50.0F);
        assertEquals(order.getTotalCost(), 3050.0F);
        verify(orderDB).update(order);

        //check for Status.PAYMENT_CHECK
        order.pay(card);
        assertEquals(Order.Status.PAYMENT_CHECK, order.getStatus());
        verify(orderDB, times(2)).update(order);

        // trigger .onError()
        ArgumentCaptor<PaymentCallback> callbackCaptor = ArgumentCaptor.forClass(PaymentCallback.class);
        verify(paymentService).pay(any(Card.class), anyFloat(), callbackCaptor.capture());
        callbackCaptor.getValue().onError("123");

        assertEquals(Order.Status.PAYMENT_ERROR, order.getStatus());
        verify(orderDB, times(3)).update(order);
    }

    @Test
    void testPaymentRetrySuccess() {
        Address mockAddress = new Address("name", "line1", "line2", "district", "city", "postcode");
        when(orderDB.getOrderID()).thenReturn(1);

        order.place("John", "Appl Watch", 2, mockAddress);
        assertEquals(Order.Status.PLACED, order.getStatus());

        //payment
        Card card = new Card("123", "JohnCena", 2, 2023);
        // pho getTotalCost to work
        when(productDB.getPrice("Appl Watch")).thenReturn(1500.0F);
        when(productDB.getWeight("Appl Watch")).thenReturn(350.0F);
        when(shippingService.getPrice(mockAddress, 700.0F)).thenReturn(50.0F);
        assertEquals(order.getTotalCost(), 3050.0F);
        verify(orderDB).update(order);

        //check for Status.PAYMENT_CHECK
        order.pay(card);
        assertEquals(Order.Status.PAYMENT_CHECK, order.getStatus());
        verify(orderDB, times(2)).update(order);

        // trigger .onError()
        ArgumentCaptor<PaymentCallback> callbackCaptor = ArgumentCaptor.forClass(PaymentCallback.class);

        verify(paymentService).pay(any(Card.class), anyFloat(), callbackCaptor.capture());
        callbackCaptor.getValue().onError("123");

        assertEquals(Order.Status.PAYMENT_ERROR, order.getStatus());
        verify(orderDB, times(3)).update(order);

        //retry payment
        order.pay(card);
        assertEquals(Order.Status.PAYMENT_CHECK, order.getStatus());
        verify(orderDB, times(4)).update(order);

        // successful this time
        callbackCaptor.getValue().onSuccess("123");

        assertEquals(Order.Status.PAID, order.getStatus());
        assertEquals(order.paymentConfirmCode, "123");
        verify(orderDB, times(5)).update(order);
    }

    @Test
    void testShippingOrder() {
        Address mockAddress = new Address("name", "line1", "line2", "district", "city", "postcode");
        when(orderDB.getOrderID()).thenReturn(1);

        order.place("John", "Appl Watch", 2, mockAddress);
        assertEquals(Order.Status.PLACED, order.getStatus());

        //payment
        Card card = new Card("123", "JohnCena", 2, 2023);
        // pho getTotalCost to work
        when(productDB.getPrice("Appl Watch")).thenReturn(1500.0F);
        when(productDB.getWeight("Appl Watch")).thenReturn(350.0F);
        when(shippingService.getPrice(mockAddress, 700.0F)).thenReturn(50.0F);
        assertEquals(order.getTotalCost(), 3050.0F);

        //check for Status.PAYMENT_CHECK
        order.pay(card);
        assertEquals(Order.Status.PAYMENT_CHECK, order.getStatus());
        verify(orderDB, times(2)).update(order);

        // trigger .onSuccess()
        ArgumentCaptor<PaymentCallback> callbackCaptor = ArgumentCaptor.forClass(PaymentCallback.class);

        verify(paymentService).pay(any(Card.class), anyFloat(), callbackCaptor.capture());
        callbackCaptor.getValue().onSuccess("123");

        assertEquals(Order.Status.PAID, order.getStatus());
        assertEquals(order.paymentConfirmCode, "123");
        verify(orderDB, times(3)).update(order);

        // shipping
        when(shippingService.ship(mockAddress, 700.0F)).thenReturn("123");
        order.ship();

        assertEquals(order.trackingCode, "123");
        assertEquals(Order.Status.SHIPPED, order.getStatus());
        verify(orderDB, times(4)).update(order);
    }

    @Test
    void testCancelingPaidOrderSuccess() {
        Address mockAddress = new Address("name", "line1", "line2", "district", "city", "postcode");
        when(orderDB.getOrderID()).thenReturn(1);

        order.place("John", "Appl Watch", 2, mockAddress);
        assertEquals(Order.Status.PLACED, order.getStatus());

        //payment
        Card card = new Card("123", "JohnCena", 2, 2023);
        // pho getTotalCost to work
        when(productDB.getPrice("Appl Watch")).thenReturn(1500.0F);
        when(productDB.getWeight("Appl Watch")).thenReturn(350.0F);
        when(shippingService.getPrice(mockAddress, 700.0F)).thenReturn(50.0F);
        assertEquals(order.getTotalCost(), 3050.0F);

        //check for Status.PAYMENT_CHECK
        order.pay(card);
        assertEquals(Order.Status.PAYMENT_CHECK, order.getStatus());
        verify(orderDB, times(2)).update(order);

        // trigger .onSuccess()
        ArgumentCaptor<PaymentCallback> callbackCaptor = ArgumentCaptor.forClass(PaymentCallback.class);

        verify(paymentService).pay(any(Card.class), anyFloat(), callbackCaptor.capture());
        callbackCaptor.getValue().onSuccess("123");

        assertEquals(Order.Status.PAID, order.getStatus());
        assertEquals(order.paymentConfirmCode, "123");
        verify(orderDB, times(3)).update(order);

        // refund
        order.refund();

        assertEquals(Order.Status.AWAIT_REFUND, order.getStatus());
        verify(orderDB, times(4)).update(order);

        //refund from payment service
        ArgumentCaptor<PaymentCallback> callbackCaptorRefund = ArgumentCaptor.forClass(PaymentCallback.class);

        verify(paymentService).refund(any(), callbackCaptorRefund.capture());
        callbackCaptorRefund.getValue().onSuccess("123");

        assertEquals(Order.Status.REFUNDED, order.getStatus());
        verify(orderDB, times(5)).update(order);
    }

    @Test
    void testCancelingPaidOrderError() {

    }

}