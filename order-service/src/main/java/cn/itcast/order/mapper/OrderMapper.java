package cn.itcast.order.mapper;

import cn.itcast.order.entity.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author 虎哥
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
