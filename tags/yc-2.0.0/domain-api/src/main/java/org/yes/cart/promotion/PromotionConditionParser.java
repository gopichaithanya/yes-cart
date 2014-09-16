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

package org.yes.cart.promotion;

import org.yes.cart.domain.entity.Promotion;

/**
 * User: denispavlov
 * Date: 13-10-19
 * Time: 1:51 PM
 */
public interface PromotionConditionParser {

    /**
     * Parse promotion domain object to create executable
     * eligibility condition object.
     *
     * @param promotion promotion
     *
     * @return promotion condition object
     */
    PromotionCondition parse(Promotion promotion);

}