const fs = require('fs');

// Read the XML content
const xmlPath = 'E:\\Desktop\\code\\word\\document.xml';
const xmlContent = fs.readFileSync(xmlPath, 'utf8');

// Extract all text content
const textRegex = /<w:t[^>]*>([^<]*)<\/w:t>/g;
const paraRegex = /<w:p[ >][\s\S]*?<\/w:p>/g;

const paras = xmlContent.match(paraRegex) || [];

const result = paras.map(para => {
    const texts = [];
    let match;
    const regex = /<w:t[^>]*>([^<]*)<\/w:t>/g;
    while ((match = regex.exec(para)) !== null) {
        if (match[1]) {
            texts.push(match[1]);
        }
    }
    return texts.join('');
}).filter(t => t.trim());

// Save to file
const outputPath = 'E:\\Desktop\\code\\docx_content.txt';
fs.writeFileSync(outputPath, result.join('\n\n'), 'utf8');
console.log('Extracted', result.length, 'paragraphs');
console.log('Saved to:', outputPath);

// Also print the content
console.log('\n--- Content ---\n');
console.log(result.join('\n\n'));
