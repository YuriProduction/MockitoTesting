package shopping;

import customer.Customer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import product.Product;
import product.ProductDao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@ExtendWith(MockitoExtension.class)
class ShoppingServiceTest
{
    //кстати я не понял, когда вообще бросается BuyException, мы же когда в корзину добавляем
    //продукт там если недостаточно выбрасывается IllegalArgumentException

    private final ProductDao productDaoMock;

    private final ShoppingService shoppingService;
    private final Customer customer;
    private Cart cart;

    @BeforeEach
    public void setUp()
    {
        cart = new Cart(customer);//я не понял почему я могу здесь вызвать конструктор
        //у Cart же пакетный уровень доступа,
        // по идее я же корзину должен через shoppingService получать???
    }

    public ShoppingServiceTest(@Mock ProductDao productDaoMock)
    {
        this.productDaoMock = productDaoMock;
        shoppingService = new ShoppingServiceImpl(productDaoMock);
        customer = new Customer(1, "+79123487654");
    }

    /**
     * Тестирование корректной покупки
     */
    @Test
    public void testCorrectPayment() throws BuyException
    {
        //товары в магазине
        Product product1 = new Product("Котлеты говяжьи", 3);
        Product product2 = new Product("Молоко", 3);
        cart.add(product1, 2);
        cart.add(product2, 2);

        Assertions.assertTrue(shoppingService.buy(cart));
        //проверяем, что оба товара (остаток) были сохранены в БД по разу
        Mockito.verify(productDaoMock, Mockito.times(1))
                .save(product1);
        Mockito.verify(productDaoMock, Mockito.times(1))
                .save(Mockito.argThat(product ->
                        product.getName().equals("Молоко")));
    }

    /**
     * Проверяем, что метод getCart по пользователю возвращает
     * соответствующую ему корзину. В текущей реализации тест валится и это норм.
     * Потому что иначе я вообще не вижу смысла в методе getCart(Customer customer).
     * Или это типо чтоб получить корзину, которую мы извне не можем получить, т.к. там пакетный доступ?
     */
    @Test
    public void testCorrectCart() throws BuyException
    {
        final Product sausage = new Product("Сосиска", 5);
        cart.add(sausage, 4);
        Assertions.assertTrue(shoppingService.buy(cart));
        assertEquals(cart, shoppingService.getCart(customer));
    }

    /**
     * Тестируем, что когда пытаемся получить все продукты, в БД вызывался соотв.метод
     */
    @Test
    public void getAllProductsTest()
    {
        shoppingService.getAllProducts();
        Mockito.verify(productDaoMock, Mockito.times(1))
                .getAll();
    }

    /**
     * Проверяем,что getProductByName возвращает корректный продукт.
     */
    @Test
    public void testGetProductByName()
    {
        final String productName = "Test Product";
        Product expectedProduct = new Product("Test Product", 10);
        Mockito.when(productDaoMock.getByName(productName)).thenReturn(expectedProduct);
        Product product = shoppingService.getProductByName(productName);
        Mockito.verify(productDaoMock, Mockito.times(1))
                .getByName("Test Product");
        assertNotNull(product);
        assertEquals(expectedProduct, product);
    }

    /**
     * Тестируем, что метод покупки возвращает false, если корзина пуста
     */
    @Test
    public void testBuyWithEmptyCart() throws BuyException {
        boolean result = shoppingService.buy(cart);
        Assertions.assertFalse(result);
        Mockito.verifyNoInteractions(productDaoMock);
    }
    /**
     * Тестовый пример для проверки того, что метод добавления выдает исключение IllegalArgumentException,
     * когда товара недостаточно.
     */
    @Test
    public void testBuyWithInsufficientQuantity() {
        Product product = new Product("Product", 5);
        Assertions.assertThrows(IllegalArgumentException.class, () -> cart.add(product, 10));
        Mockito.verifyNoInteractions(productDaoMock);
    }
}
