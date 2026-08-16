package com.hmall.tts.volcengine.docx;

import com.hmall.tts.volcengine.dto.TextSegment;
import com.hmall.tts.volcengine.dto.VoiceConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Word文档解析器
 * 使用Apache POI解析.docx文档，识别加粗和非加粗文本
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Slf4j
@Component
public class WordDocumentParser {
    
    /**
     * 解析Word文档
     * 
     * @param inputStream Word文档输入流
     * @param voiceConfig 音色配置
     * @return 文本片段列表
     * @throws Exception 解析失败时抛出异常
     */
    public List<TextSegment> parse(InputStream inputStream, VoiceConfig voiceConfig) throws Exception {
        log.info("开始解析Word文档，音色配置: boldVoice={}, normalVoice={}", 
                voiceConfig.getBoldVoice(), voiceConfig.getNormalVoice());
        
        List<TextSegment> segments = new ArrayList<>();
        
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            int order = 0;
            int paragraphId = 0;  // ✅ 段落ID计数器
            
            // 遍历所有段落
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                // 跳过空段落
                if (paragraph.getText().trim().isEmpty()) {
                    continue;
                }
                
                // 跳过标题（可选）
                String style = paragraph.getStyle();
                if (style != null && style.startsWith("Heading")) {
                    log.debug("跳过标题段落: {}", paragraph.getText());
                    continue;
                }
                
                // ✅ 每个段落递增ID
                paragraphId++;
                
                // 遍历段落中的Run（格式化文本片段）
                for (XWPFRun run : paragraph.getRuns()) {
                    String text = run.getText(0);
                    
                    // 跳过空文本
                    if (text == null || text.trim().isEmpty()) {
                        continue;
                    }
                    
                    // 判断是否加粗
                    Boolean isBold = run.isBold();
                    
                    // 根据是否加粗选择音色
                    String speaker;
                    if (isBold != null && isBold) {
                        speaker = voiceConfig.getBoldVoice();
                    } else {
                        speaker = voiceConfig.getNormalVoice();
                    }
                    
                    // 创建文本片段
                    TextSegment segment = TextSegment.builder()
                            .text(text)
                            .speaker(speaker)
                            .isBold(isBold)
                            .order(order++)
                            .paragraphId(paragraphId)  // ✅ 设置段落ID
                            .build();
                    
                    segments.add(segment);
                    
                    log.debug("解析文本片段: text={}, isBold={}, speaker={}, paragraphId={}", 
                            text, isBold, speaker, paragraphId);
                }
            }
            
            log.info("Word文档解析完成，共解析{}个文本片段", segments.size());
            
            // ✅ 诊断日志：打印所有segment的详细信息
            log.info("=== Word解析详细信息 ===");
            log.info("段落总数：{}", paragraphId);
            for (int i = 0; i < segments.size(); i++) {
                TextSegment seg = segments.get(i);
                log.info("Segment[{}]: 段落ID={}, isBold={}, 文本='{}'", 
                         i, seg.getParagraphId(), seg.getIsBold(), seg.getText());
            }
            log.info("=== Word解析详细信息结束 ===");
            
            return segments;
            
        } catch (Exception e) {
            log.error("Word文档解析失败: {}", e.getMessage(), e);
            throw new Exception("Word文档解析失败: " + e.getMessage(), e);
        }
    }
}
