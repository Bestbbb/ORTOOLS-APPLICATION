package demo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRequirement {

    // 资源需求对象ID
    private String id;
    // 资源需求对象指向的资源ID，resourcePool(资源池）中，必须有该ID对应的资源对象。
    private String resourceId;
    // 需要多少资源量
    private Integer requirement;
}
