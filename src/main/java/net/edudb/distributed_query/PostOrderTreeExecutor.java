/*
EduDB is made available under the OSI-approved MIT license.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package net.edudb.distributed_query;

import net.edudb.distributed_operator.DistributedOperator;
import net.edudb.distributed_executor.*;
import net.edudb.relation.Relation;

/**
 * This strategy executes each relational operator and returns the result as a
 * volatile relation which is not intended to be saved to disk. However, there
 * is a drawback. After executing each operator, the relations are not removed
 * from memory or disk and therefore are persisted whenever a page is replaced
 * or the system exits. You can remove the relation after being used to make
 * sure that they are not persisted to disk.
 *
 * @see PostOrderOperatorExecutor
 *
 * @author Ahmed Abdul Badie
 *
 */
public class PostOrderTreeExecutor implements QueryTreeExecutor {

	@Override
	public Relation execute(QueryTree queryTree) {
		PostOrderOperatorExecutor executor = new PostOrderOperatorExecutor();
		OperatorExecutionChain chain = executor.getChain();
		DistributedOperator operator = (DistributedOperator) queryTree.getRoot();
		chain.execute(operator);
		return null;
	}

}
