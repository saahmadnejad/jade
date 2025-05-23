/**
 * ***************************************************************
 * JADE - Java Agent DEvelopment Framework is a framework to develop
 * multi-agent systems in compliance with the FIPA specifications.
 * Copyright (C) 2000 CSELT S.p.A.
 * 
 * GNU Lesser General Public License
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation,
 * version 2.1 of the License.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 * **************************************************************
 */
package ir.donbee.jade.content.abs;

import ir.donbee.jade.content.ContentElement;

/**
 * An abstract descriptor that can hold a generic content element
 * expression.
 * @author Federico Bergenti - Universita` di Parma
 */
public interface AbsContentElement extends AbsObject, ContentElement {
	/**
	 * Return true if this Abstract Content Element represents a ContentExpression
	 * of the SL Grammar (see also FIPA-SL specifications).
	 * @return true if this Abstract Content Element represents a ContentExpression
	 * of the SL Grammar (see also FIPA-SL specifications), false otherwise 
	 */
	public boolean isAContentExpression();
	/**
	 * Set the isAContentExpression flag to the passed value.
	 * By default, if this method was not called, this value is intialized to false.
	 * @param flag true if this Abstract Content Element represents a ContentExpression
	 * of the SL Grammar (see also FIPA-SL specifications), false otherwise 
	 */
	public void setIsAContentExpression(boolean flag);
}
