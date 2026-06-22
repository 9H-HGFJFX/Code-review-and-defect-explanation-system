const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

// Simple ZIP/DEFLATE reader
function extractDocx(docxPath, outputDir) {
    try {
        const data = fs.readFileSync(docxPath);
        
        // ZIP file structure:
        // [local file header + data]...
        // [central directory]...
        // [end of central directory]
        
        let offset = 0;
        const files = {};
        
        // Find end of central directory
        let eocdOffset = -1;
        for (let i = data.length - 22; i >= 0; i--) {
            if (data[i] === 0x50 && data[i+1] === 0x4B && data[i+2] === 0x05 && data[i+3] === 0x06) {
                eocdOffset = i;
                break;
            }
        }
        
        if (eocdOffset === -1) {
            console.error('End of central directory not found');
            return;
        }
        
        const numEntries = data.readUInt16LE(eocdOffset + 10);
        const cdOffset = data.readUInt32LE(eocdOffset + 16);
        
        // Read central directory
        let cdPos = cdOffset;
        const entries = [];
        
        for (let i = 0; i < numEntries; i++) {
            if (data[cdPos] !== 0x50 || data[cdPos+1] !== 0x4B || data[cdPos+2] !== 0x01 || data[cdPos+3] !== 0x02) {
                break;
            }
            
            const nameLen = data.readUInt16LE(cdPos + 28);
            const extraLen = data.readUInt16LE(cdPos + 30);
            const commentLen = data.readUInt16LE(cdPos + 32);
            const compSize = data.readUInt32LE(cdPos + 20);
            const uncompSize = data.readUInt32LE(cdPos + 24);
            const localHeaderOffset = data.readUInt32LE(cdPos + 42);
            const compressionMethod = data.readUInt16LE(cdPos + 10);
            
            const name = data.slice(cdPos + 46, cdPos + 46 + nameLen).toString('utf8');
            
            entries.push({
                name,
                compressionMethod,
                compSize,
                uncompSize,
                localHeaderOffset
            });
            
            cdPos += 46 + nameLen + extraLen + commentLen;
        }
        
        console.log('Found', entries.length, 'entries');
        
        // Look for document.xml
        const docXmlEntry = entries.find(e => e.name === 'word/document.xml');
        
        if (!docXmlEntry) {
            console.log('Available entries:');
            entries.forEach((e, i) => console.log(`  ${i}: ${e.name}`));
            return;
        }
        
        // Read local file header
        const lhPos = docXmlEntry.localHeaderOffset;
        const lhNameLen = data.readUInt16LE(lhPos + 26);
        const lhExtraLen = data.readUInt16LE(lhPos + 28);
        const lhDataOffset = lhPos + 30 + lhNameLen + lhExtraLen;
        
        let compressedData = data.slice(lhDataOffset, lhDataOffset + docXmlEntry.compSize);
        
        let xmlContent;
        if (docXmlEntry.compressionMethod === 8) {
            // DEFLATE
            xmlContent = zlib.inflateSync(compressedData).toString('utf8');
        } else if (docXmlEntry.compressionMethod === 0) {
            // STORED
            xmlContent = compressedData.toString('utf8');
        } else {
            console.log('Unknown compression method:', docXmlEntry.compressionMethod);
            return;
        }
        
        // Extract text from XML
        const textContent = extractTextFromDocXml(xmlContent);
        console.log('\n--- Document Content ---\n');
        console.log(textContent);
        
        // Save to file
        const outputPath = path.join(outputDir || '.', 'docx_content.txt');
        fs.writeFileSync(outputPath, textContent, 'utf8');
        console.log('\nSaved to:', outputPath);
        
    } catch (err) {
        console.error('Error:', err.message);
        console.error(err.stack);
    }
}

function extractTextFromDocXml(xml) {
    const texts = [];
    const regex = /<w:t[^>]*>([^<]*)<\/w:t>/g;
    let match;
    
    while ((match = regex.exec(xml)) !== null) {
        if (match[1]) {
            texts.push(match[1]);
        }
    }
    
    // Also extract paragraphs
    const paraRegex = /<w:p[ >][\s\S]*?<\/w:p>/g;
    const paras = xml.match(paraRegex) || [];
    
    const result = paras.map(para => {
        const tRegex = /<w:t[^>]*>([^<]*)<\/w:t>/g;
        let tMatch;
        const parts = [];
        while ((tMatch = tRegex.exec(para)) !== null) {
            if (tMatch[1]) {
                parts.push(tMatch[1]);
            }
        }
        return parts.join('');
    }).filter(t => t.trim()).join('\n\n');
    
    return result;
}

// Run
const docxPath = process.argv[2] || 'E:\\Desktop\\代码审查与缺陷解释系统\\概要设计说明书.docx';
console.log('Reading:', docxPath);
extractDocx(docxPath);
