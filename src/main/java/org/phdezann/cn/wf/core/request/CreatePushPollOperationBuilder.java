package org.phdezann.cn.wf.core.request;

import java.util.List;

import org.phdezann.cn.core.JsonSerializer;
import org.phdezann.cn.wf.json.push_and_poll.request.create.DataRequest;
import org.phdezann.cn.wf.json.push_and_poll.request.create.Operation;
import org.phdezann.cn.wf.json.push_and_poll.request.create.ProjectTree;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreatePushPollOperationBuilder {

    private final JsonSerializer jsonSerializer;

    @Builder
    public record CreatePushPollOperationArgs(String nodeId, //
            String parentNodeId, //
            String shareId, //
            String transactionId, //
            Long clientTimeStamp) {
    }

    public Operation build(CreatePushPollOperationArgs args) {
        var operation = new Operation();
        operation.setType("bulk_create");
        operation.setData(buildDataRequest(args));
        operation.setClientTimestamp(args.clientTimeStamp());
        operation.setExecutedBy(-1);
        return operation;
    }

    private DataRequest buildDataRequest(CreatePushPollOperationArgs args) {
        var data = new DataRequest();
        data.setParentId(args.parentNodeId());
        data.setStartingPriority(0);
        data.setProjectTrees(getProjectTreesJson(args));
        data.setForSearch(false);
        return data;
    }

    private String getProjectTreesJson(CreatePushPollOperationArgs args) {
        var projectTree = new ProjectTree();
        projectTree.setId(args.nodeId());
        projectTree.setCt(args.clientTimeStamp());
        projectTree.setCb(-1);
        return jsonSerializer.writeValue(List.of(projectTree));
    }

}
