package demo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Step {

    // 步骤ID
    private String id;
    // 步骤编号
    private String code;
    // 步骤名称
    private String name;
    // 当前资源需求组合的资源需求列表，一个资源需求组合可以有多个资源需求组成。例如加工一个任务需要一位操作工 + 一台机器，
    // 则资源需求列表中有两个对象，分别是机台与操作工。
    private List<ResourceRequirement> resourceRequirementList;
    // 任务列表
    private List<Task> taskList;

    private String productId;

}
