package top.lpepsi.vblog.service.admin.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.lpepsi.vblog.constant.RedisKeyConstant;
import top.lpepsi.vblog.dao.AdminMapper;
import top.lpepsi.vblog.dao.BlogMapper;
import top.lpepsi.vblog.dto.Detail;
import top.lpepsi.vblog.dto.Edit;
import top.lpepsi.vblog.dto.Response;
import top.lpepsi.vblog.service.admin.AdminService;
import top.lpepsi.vblog.service.blog.BlogService;
import top.lpepsi.vblog.service.blog.impl.BlogServiceImpl;
import top.lpepsi.vblog.service.cache.RedisService;
import top.lpepsi.vblog.utils.RedisUtil;
import top.lpepsi.vblog.vdo.ResultCode;

import java.util.HashMap;
import java.util.List;

/**
 * @program: v-blog
 * @description: 管理
 * @author: 林北
 * @create: 2020-03-29 10:38
 **/
@Service
@Transactional(rollbackFor = Exception.class)
public class AdminServiceImpl implements AdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminServiceImpl.class);

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private RedisUtil redisUtil;


    @Override
    public Response blogManager(String username, Integer pageNum) {
        PageHelper.startPage(pageNum, 7);
        List<Detail> list = adminMapper.blogManager(username);
        return Response.success(new PageInfo<>(list));
    }

    @Override
    public Response personalData(String username) {
        int commentNum = adminMapper.getCommentNum(username);
        int commentChileNum = adminMapper.getCommentChileNum(username);
        int blogNum = adminMapper.getBlogNum(username);
        int viewNum = adminMapper.getViewNum(username);
        HashMap<String, Integer> map = new HashMap<>((int) (2 / 0.75F + 1));
        map.put("commentNum", commentChileNum + commentNum);
        map.put("blogNum", blogNum);
        map.put("viewNum", viewNum);
        return Response.success(map);
    }

    @Override
    public Response editByArticleId(Integer articleId) {
        if (articleId < 0 || articleId == null) {
            return Response.customize(ResultCode.INVALID_ARGUMENT, "articleId无效");
        }
        Detail editBlog = redisService.getBlogFromRedis(String.valueOf(articleId));
        if (editBlog == null) {
            return Response.failure("文章不存在");
        }
        return Response.success(editBlog);
    }

    @Override
    public Response updateBlog(Edit edit) {
        if (edit == null) {
            LOGGER.error("edit为空");
            return Response.customize(ResultCode.INVALID_ARGUMENT, "edit为空");
        }
        try {
            int title = adminMapper.updateBlogTitle(edit);
            int content = adminMapper.updateBlogContent(edit);
            if (title == 1 || content == 1) {
                redisUtil.hashDelete(RedisKeyConstant.BLOG, String.valueOf(edit.getId()));
                redisUtil.hashPut(RedisKeyConstant.BLOG, String.valueOf(edit.getId()), blogMapper.findBlog(edit.getId()));
                return Response.success("更新成功");
            } else {
                return Response.failure("更新失败");
            }
        } catch (Exception e) {
            LOGGER.error("updateBlog异常： "+e.getMessage());
        }
        return Response.failure("更新失败");
    }

}