const fs = require('fs');
const path = require('path');
const readline = require('readline');

// Simple ZIP reader for DOCX
function readDocxContent(docxPath) {
    try {
        // Read the file as a buffer
        const data = fs.readFileSync(docxPath);
        
        // Check for ZIP signature
        if (data[0] !== 0x50 || data[1] !== 0x4B) {
            console.error('Not a valid ZIP/DOCX file');
            return;
        }
        
        // Find the end of central directory
        let offset = data.length - 22;
        while (offset > 0) {
            if (data[offset] === 0x50 && data[offset+1] === 0x4B && data[offset+2] === 0x05 && data[offset+3] === 0x06) {
                break;
            }
            offset--;
        }
        
        // List central directory entries
        let pos = offset + 46;
        const entries = [];
        
        // Read number of entries
        const numEntries = data.readUInt16LE(offset + 10);
        
        for (let i = 0; i < numEntries; i++) {
            if (data[pos] !== 0x50 || data[pos+1] !== 0x4B || data[pos+2] !== 0x01 || data[pos+3] !== 0x02) {
                break;
            }
            
            // Read filename length
            const nameLen = data.readUInt16LE(pos + 28);
            const extraLen = data.readUInt16LE(pos + 30);
            const commentLen = data.readUInt16LE(pos + 32);
            
            // Extract filename
            const name = data.slice(pos + 46, pos + 46 + nameLen).toString('utf8');
            entries.push(name);
            
            pos += 46 + nameLen + extraLen + commentLen;
        }
        
        console.log('Files in DOCX:');
        entries.forEach(e => console.log(' ', e));
        
        // Try to read word/document.xml
        const docXmlPath = 'word/document.xml';
        const docXmlIndex = entries.indexOf(docXmlPath);
        
        if (docXmlIndex === -1) {
            console.log('\nword/document.xml not found');
            return;
        }
        
        console.log('\nReading document.xml...');
        
    } catch (err) {
        console.error('Error:', err.message);
    }
}

const docxPath = process.argv[2] || 'E:\\Desktop\\代码审查与缺陷解释系统\\概要设计说明书.docx';
console.log('Reading:', docxPath);
readDocxContent(docxPath);
