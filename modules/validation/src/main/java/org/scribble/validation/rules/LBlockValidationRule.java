/*
 * Copyright 2009-11 www.scribble.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.scribble.validation.rules;

import org.scribble.common.logging.ScribbleLogger;
import org.scribble.common.module.ModuleContext;
import org.scribble.model.ModelObject;
import org.scribble.model.local.LActivity;
import org.scribble.model.local.LBlock;

/**
 * This class implements the validation rule for the LBlock
 * component.
 *
 */
public class LBlockValidationRule implements ValidationRule {

	/**
	 * {@inheritDoc}
	 */
	public void validate(ModuleContext context, ModelObject mobj, ScribbleLogger logger) {
		LBlock elem=(LBlock)mobj;
		
		for (LActivity act : elem.getContents()) {
			ValidationRule rule=ValidationRuleFactory.getValidationRule(act);
			
			if (rule != null) {
				rule.validate(context, act, logger);
			}
		}
	}

}