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


@ExtendWith(MockitoExtension.class)
class ShoppingServiceTest
{
    //Кстати я не понял, когда вообще бросается BuyException, мы же когда в корзину добавляем
    //продукт, там, если недостаточно товара, выбрасывается IllegalArgumentException

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
        Product product2 = new Product("Молоко", 4);
        cart.add(product1, 2);
        cart.add(product2, 2);

        Assertions.assertTrue(shoppingService.buy(cart));
        //проверяем, что оба товара (остаток) были сохранены в БД по разу
        Mockito.verify(productDaoMock, Mockito.times(1))
                .save(product1);
        Mockito.verify(productDaoMock, Mockito.times(1))
                .save(Mockito.argThat(product ->
                        product.getName().equals("Молоко")));
        //проверим, что количество доступных товаров уменьшилось:
        Assertions.assertEquals(1, product1.getCount());
        Assertions.assertEquals(2, product2.getCount());
        //Проверим, что корзина очистилась
        Assertions.assertTrue(shoppingService.getCart(customer).getProducts().isEmpty());
        //Проверим вообще, что корзина совпадают
        Assertions.assertSame(shoppingService.getCart(customer), cart);
        //Тестируем выброс BuyException
        Assertions.assertThrows(BuyException.class, () ->
                {
                    //Проверяем выброс исключения
                    Product product3 = new Product("Сок", 4);
                    cart.add(product3, 2);
                    product3.subtractCount(3); //моделируем ситуацию, когда другой человек купил 3 сока
                    //Вообще, тест странный, т.к. как другой покупатель может взять 3 сока,
                    // когда уже добавлены 2 сока нами в корзину, а всего 4 сока
                    //Но непонятно, как по-другому тестировать
                }
        );
    }

    /**
     * Проверяем, что метод getCart по пользователю возвращает
     * соответствующую ему корзину. В текущей реализации тест валится и это норм.
     * Потому что иначе я вообще не вижу смысла в методе getCart(Customer customer).
     * Или это типо, чтоб получить корзину, которую мы извне не можем получить, т.к. там пакетный доступ?
     */
    @Test
    public void testCorrectCart()
    {
        assertEquals(cart, shoppingService.getCart(customer));
    }


    @Test
    public void getAllProductsTest()
    {
        //Не надо тестировать, т.к. в ShoppingServiceImpl нет никакой логики,
        //которая бы доходила до этого метода.
    }

    /**
     * Проверяем, что getProductByName возвращает корректный продукт.
     */
    @Test
    public void testGetProductByName()
    {
        //Не надо тестировать, т.к. нет логики для getProductByName
    }

    /**
     * Тестируем, что метод покупки возвращает false, если корзина пуста
     */
    @Test
    public void testBuyWithEmptyCart() throws BuyException
    {
        boolean result = shoppingService.buy(cart);
        Assertions.assertFalse(result);
        Mockito.verifyNoInteractions(productDaoMock);
    }

    /**
     * Тестовый пример для проверки того, что метод добавления выдает исключение IllegalArgumentException,
     * когда товара недостаточно.
     */
    @Test
    public void testBuyWithInsufficientQuantity()
    {
        Product product = new Product("Product", 5);
        Assertions.assertThrows(IllegalArgumentException.class, () -> cart.add(product, 10));
        Mockito.verifyNoInteractions(productDaoMock);
    }
}
