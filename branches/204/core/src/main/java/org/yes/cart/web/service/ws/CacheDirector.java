/*
 * Copyright 2013 Igor Azarnyi, Denys Pavlov
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
package org.yes.cart.web.service.ws;


import org.yes.cart.domain.dto.impl.CacheInfoDTOImpl;

import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.jws.WebService;
import java.util.List;

/**
 * Service responsible  to evict particular cache(s) depending on entity and operation.
 *
 * User: Igor Azarny iazarny@yahoo.com
 * Date: 18 Aug 2013
 * Time: 9:50 AM
 */
@WebService
public interface CacheDirector {

    public interface EntityName {

        String ATTRIBUTE = "AttributeEntity";
        String CATEGORY = "CategoryEntity";
        String SEO_IMAGE = "SeoImageEntity";
        String PRICE = "SkuPriceEntity";
        String PRODUCT = "ProductEntity";

        String PRODUCT_TYPE_ATTR = "ProductTypeAttrEntity";
        String SHOP = "ShopEntity";
        String SYSTEM = "SystemEntity";

    }

    public interface EntityOperation {

        String CREATE = "Create";
        String DELETE = "Delete";
        String UPDATE = "Update";

    }


    /**
     * Get cache information.
     * @return list of information per each cache.
     */
    @WebMethod
    @WebResult(name = "cacheInfoResult")
    List<CacheInfoDTOImpl> getCacheInfo();

    /**
     * Evict all caches , which are represent in getCacheInfo list.
     */
    @WebMethod
    @WebResult(name = "cacheInfoResult")
    void evictCache();

    /**
     *
     * @param entityOperation
     * @param entityName
     * @param pkValue
     */
    @WebMethod
    @WebResult(name = "itemCount")
    int onCacheableChange(String entityOperation, String entityName, Long pkValue);

}
