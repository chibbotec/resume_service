import requests
from bs4 import BeautifulSoup
from urllib.parse import urlparse
import time
import re

# =========================
# 메인 크롤러
# =========================
class JobCrawler:
    def __init__(self):
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }
    
    def get_site_name(self, url):
        """URL에서 사이트명 추출"""
        domain = urlparse(url).netloc.lower()
        
        if 'jumpit' in domain:
            return 'jumpit'
        elif 'saramin' in domain:
            return 'saramin'
        elif 'jobkorea' in domain:
            return 'jobkorea'
        elif 'wanted' in domain:
            return 'wanted'
        elif 'programmers' in domain:
            return 'programmers'
        else:
            return 'unknown'
    
    def fetch_html(self, url):
        """HTML 가져오기"""
        try:
            response = requests.get(url, headers=self.headers)
            response.raise_for_status()
            return response.text
        except Exception as e:
            print(f"HTML 가져오기 실패: {e}")
            return None
    
    def crawl(self, url):
        """메인 크롤링 함수"""
        print(f"크롤링 시작: {url}")
        
        # 사이트 구분
        site_name = self.get_site_name(url)
        print(f"사이트: {site_name}")
        
        # HTML 가져오기
        html = self.fetch_html(url)
        if not html:
            return None
        
        # 사이트별 크롤링 모듈 호출
        result = self.crawl_by_site(site_name, html, url)
        return result
    
    def crawl_by_site(self, site_name, html, url):
        """사이트별 크롤링 모듈 호출"""
        print(f"{site_name} 전용 크롤링 실행")
        
        if site_name == 'jumpit':
            crawler = JumpitCrawler()
            return crawler.extract_raw_data(html, url)
        elif site_name == 'saramin':
            crawler = SaraminCrawler()
            return crawler.extract_raw_data(html, url)
        elif site_name == 'jobkorea':
            crawler = JobkoreaCrawler()
            return crawler.extract_raw_data(html, url)
        elif site_name == 'wanted':
            crawler = WantedCrawler()
            return crawler.extract_raw_data(html, url)
        else:
            return {
                'site': site_name,
                'url': url,
                'status': 'error',
                'message': f'{site_name} 크롤러가 아직 구현되지 않았습니다.'
            }

# =========================
# 점핏 크롤러 모듈
# =========================
class JumpitCrawler:
    def extract_raw_data(self, html: str, url: str) -> dict:
        """점핏에서 raw 데이터 추출"""
        soup = BeautifulSoup(html, 'html.parser')
        
        # 불필요한 태그 제거
        for tag in soup(["script", "style", "noscript", "header", "nav", "footer"]):
            tag.decompose()
        
        # 메인 콘텐츠만
        main = soup.find('main') or soup
        
        # 텍스트 추출 및 정리
        text = main.get_text()
        text = re.sub(r'\s+', ' ', text)  # 공백 정리
        text = re.sub(r'\n+', '\n', text)  # 줄바꿈 정리
        
        return {
            'site': 'jumpit',
            'url': url,
            'status': 'success',
            'raw_data': text.strip()
        }

class SaraminCrawler:
    def extract_raw_data(self, html: str, url: str) -> dict:
        """사람인에서 raw 데이터 추출"""
        # TODO: 사람인 전용 로직 구현
        return {
            'site': 'saramin',
            'url': url,
            'status': 'not_implemented',
            'message': '사람인 크롤러 구현 예정'
        }

class JobkoreaCrawler:
    def extract_raw_data(self, html: str, url: str) -> dict:
        """잡코리아에서 raw 데이터 추출"""
        soup = BeautifulSoup(html, 'html.parser')
        
        try:
            # 불필요한 요소 제거
            for tag in soup(["script", "style", "noscript", "header", "nav", "footer", "aside"]):
                tag.decompose()
            
            # 광고 관련 요소 제거
            for ad_class in ['.footer__advertisement', '#adTopWrap', '#adLeftWing', '#adRightWing', '.adWing']:
                for element in soup.select(ad_class):
                    element.decompose()
            
            # 메인 컨테이너 찾기
            container = soup.find('section', {'id': 'container'})
            if not container:
                container = soup.find('div', {'id': 'wrap'})
            if not container:
                container = soup.find('body')
            
            if not container:
                return {
                    'site': 'jobkorea',
                    'url': url,
                    'status': 'error',
                    'message': '메인 컨테이너를 찾을 수 없습니다.'
                }
            
            # 주요 섹션들의 텍스트 수집
            collected_texts = []
            
            # 1. 채용 요약 정보
            summary_section = container.find('section', class_='secReadSummary')
            if summary_section:
                summary_text = self._extract_clean_text(summary_section)
                if summary_text:
                    collected_texts.append("=== 채용 요약 정보 ===")
                    collected_texts.append(summary_text)
            
            # 2. 상세 요강
            detail_section = container.find('section', class_='secReadDetail')
            if detail_section:
                detail_text = self._extract_clean_text(detail_section)
                if detail_text:
                    collected_texts.append("=== 상세 요강 ===")
                    collected_texts.append(detail_text)
            
            # 3. 접수기간/방법
            apply_section = container.find('section', class_='secReadApply')
            if apply_section:
                apply_text = self._extract_clean_text(apply_section)
                if apply_text:
                    collected_texts.append("=== 접수기간/방법 ===")
                    collected_texts.append(apply_text)
            
            # 4. 기업정보
            company_section = container.find('section', class_='secReadCoInfo')
            if company_section:
                company_text = self._extract_clean_text(company_section)
                if company_text:
                    collected_texts.append("=== 기업정보 ===")
                    collected_texts.append(company_text)
            
            # 5. 근무환경
            work_section = container.find('section', class_='secReadWork')
            if work_section:
                work_text = self._extract_clean_text(work_section)
                if work_text:
                    collected_texts.append("=== 근무환경 ===")
                    collected_texts.append(work_text)
            
            # 6. iframe 내용 처리 (상세요강이 iframe에 있는 경우)
            iframe = container.find('iframe', {'id': 'gib_frame'})
            if iframe and iframe.get('src'):
                iframe_text = "상세요강은 별도 페이지에서 확인 가능"
                collected_texts.append("=== 상세요강 (iframe) ===")
                collected_texts.append(iframe_text)
            
            # 텍스트가 수집되지 않은 경우 전체 컨테이너에서 추출
            if not collected_texts:
                collected_texts.append(self._extract_clean_text(container))
            
            # 최종 텍스트 결합
            final_text = '\n\n'.join(filter(None, collected_texts))
            
            if not final_text.strip():
                return {
                    'site': 'jobkorea',
                    'url': url,
                    'status': 'error',
                    'message': '추출된 텍스트가 없습니다.'
                }
            
            return {
                'site': 'jobkorea',
                'url': url,
                'status': 'success',
                'raw_data': final_text.strip()
            }
            
        except Exception as e:
            return {
                'site': 'jobkorea',
                'url': url,
                'status': 'error',
                'message': f'크롤링 중 오류 발생: {str(e)}'
            }
    
    def _extract_clean_text(self, element):
        """요소에서 깨끗한 텍스트 추출"""
        if not element:
            return ""
        
        # 불필요한 하위 요소 제거
        for tag in element.find_all(["script", "style", "noscript"]):
            tag.decompose()
        
        # 텍스트 추출
        text = element.get_text(separator=' ', strip=True)
        
        # 텍스트 정리
        text = re.sub(r'\s+', ' ', text)  # 연속된 공백을 하나로
        text = re.sub(r'\n+', '\n', text)  # 연속된 줄바꿈을 하나로
        text = text.strip()
        
        return text

class WantedCrawler:
    def extract_raw_data(self, html: str, url: str) -> dict:
        """원티드에서 raw 데이터 추출"""
        soup = BeautifulSoup(html, 'html.parser')
        
        # JSON에서 raw 텍스트만 추출
        json_text = self._extract_json_raw_text(soup)
        if json_text:
            return {
                'site': 'wanted',
                'url': url,
                'status': 'success',
                'raw_data': json_text
            }
        
        # JSON 실패시 HTML에서 raw 텍스트 추출
        for tag in soup(["script", "style", "noscript", "nav", "footer", "aside"]):
            tag.decompose()
        
        main = soup.find('main') or soup.find('body')
        
        if main:
            text = main.get_text()
            text = re.sub(r'\s+', ' ', text)
            text = re.sub(r'\n+', '\n', text)
            text = text.strip()
            
            return {
                'site': 'wanted',
                'url': url,
                'status': 'success',
                'raw_data': text
            }
        
        return {
            'site': 'wanted',
            'url': url,
            'status': 'error',
            'message': '콘텐츠를 찾을 수 없습니다.'
        }
    
    def _extract_json_raw_text(self, soup):
        """JSON에서 모든 텍스트 내용을 raw로 추출"""
        try:
            import json
            json_script = soup.find('script', {'id': '__NEXT_DATA__'})
            if json_script:
                data = json.loads(json_script.string)
                initial_data = data.get('props', {}).get('pageProps', {}).get('initialData', {})
                
                if initial_data:
                    # 모든 텍스트 필드를 그냥 이어붙이기
                    all_text = []
                    
                    def extract_all_strings(obj):
                        if isinstance(obj, str):
                            all_text.append(obj)
                        elif isinstance(obj, dict):
                            for value in obj.values():
                                extract_all_strings(value)
                        elif isinstance(obj, list):
                            for item in obj:
                                extract_all_strings(item)
                    
                    extract_all_strings(initial_data)
                    
                    # 모든 텍스트를 합쳐서 반환
                    raw_text = ' '.join(all_text)
                    raw_text = re.sub(r'\s+', ' ', raw_text)
                    return raw_text.strip()
                    
        except Exception as e:
            print(f"JSON 추출 오류: {e}")
            
        return None

# =========================
# 사용 함수
# =========================
def crawl_url(url):
    """URL을 입력받아서 크롤링"""
    crawler = JobCrawler()
    result = crawler.crawl(url)
    return result

if __name__ == "__main__":
    # 테스트용 URL들
    test_urls = [
        "https://www.jobkorea.co.kr/Recruit/GI_Read/46733476",  # 잡코리아
        "https://www.wanted.co.kr/wd/284408",  # 원티드
        # "https://jumpit.saramin.co.kr/position/50843"  # 점핏
    ]
    
    for url in test_urls:
        print(f"\n{'='*80}")
        print(f"테스트 URL: {url}")
        print('='*80)
        
        result = crawl_url(url)
        
        if result and result['status'] == 'success':
            print(f"사이트: {result['site']}")
            print(f"상태: 성공")
            print(f"데이터 길이: {len(result['raw_data'])} 문자")
            print("\n[추출된 데이터 미리보기]")
            print(result['raw_data'][:500] + "..." if len(result['raw_data']) > 500 else result['raw_data'])
        else:
            print(f"오류: {result.get('message', '알 수 없는 오류') if result else 'result가 None입니다'}")
