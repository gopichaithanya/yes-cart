/*
 * Copyright 2009 Igor Azarnyi, Denys Pavlov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.yes.cart.service.order.impl.handler;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.yes.cart.constants.Constants;
import org.yes.cart.domain.entity.*;
import org.yes.cart.payment.dto.Payment;
import org.yes.cart.service.domain.ProductService;
import org.yes.cart.service.domain.SkuWarehouseService;
import org.yes.cart.service.domain.WarehouseService;
import org.yes.cart.service.order.*;
import org.yes.cart.service.order.impl.OrderEventImpl;
import org.yes.cart.service.payment.PaymentProcessor;
import org.yes.cart.service.payment.PaymentProcessorFactory;
import org.yes.cart.util.MoneyUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * Initial {@link CustomerOrder#ORDER_STATUS_PENDING} state.
 * <p/>
 * User: Igor Azarny iazarny@yahoo.com
 * Date: 09-May-2011
 * Time: 14:12:54
 */
public class PendingOrderEventHandlerImpl extends AbstractOrderEventHandlerImpl implements OrderEventHandler, ApplicationContextAware {

    private final PaymentProcessorFactory paymentProcessorFactory;
    private OrderStateManager orderStateManager = null;
    private ApplicationContext applicationContext;
    private final WarehouseService warehouseService;
    private final SkuWarehouseService skuWarehouseService;
    private final ProductService productService;

    /**
     * Construct transition handler.
     *
     * @param paymentProcessorFactory to perform authorize operation
     * @param warehouseService        warehouse service
     * @param skuWarehouseService     sku on warehouse service to change quantity
     * @param productService          product service
     */
    public PendingOrderEventHandlerImpl(final PaymentProcessorFactory paymentProcessorFactory,
                                        final WarehouseService warehouseService,
                                        final SkuWarehouseService skuWarehouseService,
                                        final ProductService productService) {
        this.paymentProcessorFactory = paymentProcessorFactory;
        this.warehouseService = warehouseService;
        this.skuWarehouseService = skuWarehouseService;
        this.productService = productService;
    }

    /**
     * {@inheritDoc}
     */
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private synchronized OrderStateManager getOrderStateManager() {
        if (orderStateManager == null) {
            orderStateManager = (OrderStateManager) applicationContext.getBean("orderStateManager");
        }
        return orderStateManager;
    }

    /**
     * {@inheritDoc}
     */
    public boolean handle(final OrderEvent orderEvent) throws OrderException {
        synchronized (OrderEventHandler.syncMonitor) {

            for (CustomerOrderDelivery customerOrderDelivery : orderEvent.getCustomerOrder().getDelivery()) {
                reserveQuantity(customerOrderDelivery);
            }
            handleInternal(orderEvent);

            final CustomerOrder order = orderEvent.getCustomerOrder();
            final PaymentProcessor paymentProcessor = paymentProcessorFactory.create(order.getPgLabel(), order.getShop().getCode());
            if (paymentProcessor.getPaymentGateway().getPaymentGatewayFeatures().isOnlineGateway()) {
                if (Payment.PAYMENT_STATUS_OK.equals(paymentProcessor.authorize(orderEvent.getCustomerOrder(), orderEvent.getParams()))) {
                    //payment was ok, so quantity on warehouses will be decreased
                    getOrderStateManager().fireTransition(new OrderEventImpl(OrderStateManager.EVT_PAYMENT_OK, orderEvent.getCustomerOrder()));
                } else {
                    //in case of bad payment reserved product quantity will be returned from reservation
                    getOrderStateManager().fireTransition(new OrderEventImpl(OrderStateManager.EVT_CANCEL, orderEvent.getCustomerOrder()));
                }
            } else {
                // wait for confirmation about payment
                paymentProcessor.authorize(orderEvent.getCustomerOrder(), orderEvent.getParams());
                getOrderStateManager().fireTransition(new OrderEventImpl(OrderStateManager.EVT_PAYMENT_OFFLINE, orderEvent.getCustomerOrder()));
            }

            return true;
        }
    }

    /**
     * Allocate sku quantity on warehouses, that belong to shop, where order was made.
     *
     * @param orderDelivery reserve for this delivery
     * @throws OrderItemAllocationException in case if can not allocate quantity for each sku
     */
    void reserveQuantity(final CustomerOrderDelivery orderDelivery) throws OrderItemAllocationException {


        final Collection<CustomerOrderDeliveryDet> deliveryDetails = orderDelivery.getDetail();

        final List<Warehouse> warehouses = warehouseService.getByShopId(
                orderDelivery.getCustomerOrder().getShop().getShopId());


        for (CustomerOrderDeliveryDet det : deliveryDetails) {
            String skuCode = det.getProductSkuCode();
            BigDecimal toReserve = det.getQty();
            for (Warehouse warehouse : warehouses) {

                toReserve = skuWarehouseService.reservation(warehouse, skuCode, toReserve);
                if (BigDecimal.ZERO.setScale(Constants.DEFAULT_SCALE).equals(toReserve.setScale(Constants.DEFAULT_SCALE))) {
                    break; // quantity allocated
                }
            }


            if (MoneyUtils.isFirstBiggerThanSecond(toReserve, BigDecimal.ZERO)) {

                final Product product = productService.getProductBySkuCode(det.getProductSkuCode());

                if (product == null || Product.AVAILABILITY_STANDARD == product.getAvailability()) {

                    /**
                     * Availability.AVAILABILITY_BACKORDER -  can get more stock
                     * Availability.AVAILABILITY_PREORDER - can pre-order from manufacturer
                     * Availability.AVAILABILITY_ALWAYS - always
                     */
                    throw new OrderItemAllocationException(
                            skuCode,
                            toReserve,
                            "PendingOrderEventHandlerImpl. Can not allocate total qty = " + det.getQty()
                            + " for sku = " + skuCode
                            + " in delivery " + orderDelivery.getDeliveryNum());
                }
            }
        }
        orderDelivery.setDeliveryStatus(CustomerOrderDelivery.DELIVERY_STATUS_INVENTORY_RESERVED);

    }

    /**
     * {@inheritDoc}
     */
    protected String getTransitionTarget(final OrderEvent orderEvent) {
        return CustomerOrder.ORDER_STATUS_PENDING;
    }


}