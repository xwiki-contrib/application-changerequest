/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.changerequest.internal.strategies;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestReview;

/**
 * This strategy checks that a change request can be merged when a fixed number of approval reviews is reached.
 *
 * @version $Id$
 * @since 0.4
 */
@Component
@Named(FixedNumberApprovalsMergeApprovalStrategy.NAME)
@Singleton
public class FixedNumberApprovalsMergeApprovalStrategy extends AbstractMergeApprovalStrategy implements Initializable
{
    /**
     * Name of the strategy.
     */
    public static final String NAME = "fixednumberapprovals";

    private static final int DEFAULT_NUMBER = 3;

    private int thresholdNumber;

    /**
     * Default constructor.
     */
    public FixedNumberApprovalsMergeApprovalStrategy()
    {
        super(NAME);
    }

    @Override
    public void initialize() throws InitializationException
    {
        // FIXME: Initialize the number based on a configuration.
        this.thresholdNumber = DEFAULT_NUMBER;
    }

    @Override
    public boolean canBeMerged(ChangeRequest changeRequest)
    {
        boolean result = false;
        int approved = 0;
        for (ChangeRequestReview review : changeRequest.getReviews()) {
            if (review.isValid()) {
                if (review.isApproved()) {
                    approved++;
                }
                if (approved >= this.thresholdNumber) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    @Override
    public String getStatus(ChangeRequest changeRequest)
    {
        String result;
        if (canBeMerged(changeRequest)) {
            result = this.contextualLocalizationManager.getTranslationPlain(getTranslationPrefix() + "status.success",
                this.thresholdNumber);
        } else {
            int approved = 0;
            for (ChangeRequestReview review : changeRequest.getReviews()) {
                if (review.isApproved()) {
                    approved++;
                }
            }
            int missingReviews = this.thresholdNumber - approved;
            result = this.contextualLocalizationManager.getTranslationPlain(getTranslationPrefix() + "status.failure",
                this.thresholdNumber, missingReviews);
        }
        return result;
    }
}
