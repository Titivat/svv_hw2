package th.ac.kmitl.se;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;

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
        orderDB =  mock(OrderDB.class);
        productDB =  mock(ProductDB.class);
        paymentService =  mock(PaymentService.class);
        shippingService =  mock(ShippingService.class);
        order = Mockito.spy(new Order(orderDB, productDB, paymentService, shippingService));
    }

    @Test
    void testPlacingOrder() {
        Address mockAddress = new Address("name", "line1", "line2", "district", "city", "postcode");
        when(orderDB.getOrderID()).thenReturn(123);

        order.place("John", "Appl Watch", 2, mockAddress);
        assertEquals(Order.Status.PLACED, order.getStatus());

        when(orderDB.retrieveOrder(123)).thenReturn( order );
        assertEquals( orderDB.retrieveOrder(123), order);
    }

    @Test
    void testCancelingUnpaidOrder() {
        Address mockAddress = new Address("name", "line1", "line2", "district", "city", "postcode");
        when(orderDB.getOrderID()).thenReturn(1);

        order.place("John", "Appl Watch", 2, mockAddress);
        assertEquals(Order.Status.PLACED, order.getStatus());

        order.cancel();
        assertEquals(Order.Status.CANCELED, order.getStatus());

        when(orderDB.retrieveOrder(1)).thenReturn( order );
        assertEquals( orderDB.retrieveOrder(1), order);
    }

    @Test
    void testPaymentSuccess() {
        Address mockAddress = new Address("name", "line1", "line2", "district", "city", "postcode");
        when(orderDB.getOrderID()).thenReturn(1);

        order.place("John", "Appl Watch", 2, mockAddress);
        assertEquals(Order.Status.PLACED, order.getStatus());
    }

    @Test
    void testPaymentError() {
        //doAnswer
    }

    @Test
    void testPaymentRetrySuccess() {

    }

    @Test
    void testShippingOrder() {

    }

    @Test
    void testCancelingPaidOrderSuccess() {

    }

    @Test
    void testCancelingPaidOrderError() {

    }

}