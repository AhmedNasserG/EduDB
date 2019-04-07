/*
EduDB is made available under the OSI-approved MIT license.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package net.edudb.master.executor;

import net.edudb.data_type.DataType;
import net.edudb.data_type.VarCharType;
import net.edudb.engine.Utility;
import net.edudb.master.MasterWriter;
import net.edudb.meta_manager.MetaDAO;
import net.edudb.meta_manager.MetaManager;
import net.edudb.metadata_buffer.MetadataBuffer;
import net.edudb.response.Response;
import net.edudb.structure.Column;
import net.edudb.structure.Record;

import java.util.Hashtable;
import java.util.regex.Matcher;

/**
 * Creates a shard of a table (whether sharded or replicated)
 * and creates the shard table in the worker
 */
public class CreateShardExecutor implements MasterExecutorChain {

    private MasterExecutorChain nextElement;
    private String regex = "create shard \\((\\w+), (\\w+), (\\w+), (\\w+), (\\w+)\\)";

    @Override
    public void setNextElementInChain(MasterExecutorChain chainElement) {
        this.nextElement = chainElement;
    }

    public void execute(String string) {
        if (string.startsWith("create shard")) {
            Matcher matcher = Utility.getMatcher(string, regex);
            if (matcher.matches()) {
                String tableName = matcher.group(1);
                String workerHost = matcher.group(2);
                int workerPort = Integer.parseInt(matcher.group(3));
                String minValue = matcher.group(4);
                String maxValue = matcher.group(5);

                Hashtable<String, DataType> table = MetadataBuffer.getInstance().getTables().get(tableName);

                if (table == null) {
                    MasterWriter.getInstance().write(new Response("Table '" + tableName + "' does not exist"));
                    return;
                }

                Record worker = MetadataBuffer.getInstance().getWorkers().get(workerHost + ":" + workerPort);

                if (worker == null) {
                    MasterWriter.getInstance().write(new Response(
                            "No worker registered at " + workerHost + ":" + workerPort
                    ));
                    return;
                }

                String distributionMethod = ((VarCharType)table.get("distribution_method")).getString();
                String distributionColumn = ((VarCharType)table.get("distribution_column")).getString();
                String metadata = ((VarCharType)table.get("metadata")).getString();


                switch (distributionMethod) {
                    case "null":
                        MasterWriter.getInstance().write(new Response("Distribution method for table '" + tableName +
                                "' has not been set"));
                        return;
                    case "replication":
                        if (MetadataBuffer.getInstance().getShards().get(workerHost + ":" + workerPort + ":" + tableName
                            + ":null") != null) {
                            MasterWriter.getInstance().write(new Response("Duplicate shard exists at " + workerHost
                                    + ":" + workerPort));
                            return;
                        }
                        MetaDAO metaDAO = MetaManager.getInstance();
                        metaDAO.writeShard(workerHost, workerPort, tableName, "null", "null");
                }

            }
        }
        else {
            nextElement.execute(string);
        }

    }

}
